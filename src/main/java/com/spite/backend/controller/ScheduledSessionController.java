package com.spite.backend.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.OfflineClient;
import com.spite.backend.model.Role;
import com.spite.backend.model.ScheduledSession;
import com.spite.backend.model.User;
import com.spite.backend.repository.OfflineClientRepository;
import com.spite.backend.repository.ScheduledSessionRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.EmailService;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.PushNotificationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class ScheduledSessionController {

    private final ScheduledSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final OfflineClientRepository offlineClientRepo;
    private final SessionAuthService sessionAuthService;
    private final RoleGuardService guard;
    private final InputValidationService validation;
    private final PushNotificationService pushService;
    private final EmailService emailService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm").withZone(ZoneId.of("Europe/Belgrade"));

    public ScheduledSessionController(
            ScheduledSessionRepository sessionRepo,
            UserRepository userRepo,
            OfflineClientRepository offlineClientRepo,
            SessionAuthService sessionAuthService,
            RoleGuardService guard,
            InputValidationService validation,
            PushNotificationService pushService,
            EmailService emailService) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.offlineClientRepo = offlineClientRepo;
        this.sessionAuthService = sessionAuthService;
        this.guard = guard;
        this.validation = validation;
        this.pushService = pushService;
        this.emailService = emailService;
    }

    public static class CreateRequest {
        public long startTime;
        public int durationMinutes;
        public String note;
        public List<String> clientUsernames;
        public List<String> customNames;
        public List<String> offlineClientIds;
    }

    @PostMapping
    public ResponseEntity<?> createSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String trainerUsername,
            @RequestBody CreateRequest req) {

        if (validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid trainer username");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername)
                || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        boolean hasClients = req != null && req.clientUsernames != null && !req.clientUsernames.isEmpty();
        boolean hasCustom = req != null && req.customNames != null && !req.customNames.isEmpty();
        boolean hasOffline = req != null && req.offlineClientIds != null && !req.offlineClientIds.isEmpty();
        if (req == null || (!hasClients && !hasCustom && !hasOffline)) {
            return ResponseEntity.badRequest().body("No clients selected");
        }
        if (req.startTime <= 0) {
            return ResponseEntity.badRequest().body("Invalid start time");
        }

        int duration = req.durationMinutes > 0 ? req.durationMinutes : 60;
        String note = req.note == null ? "" : req.note.trim();
        String whenStr = FMT.format(Instant.ofEpochMilli(req.startTime));

        Set<String> clients = hasClients ? new LinkedHashSet<>(req.clientUsernames) : new LinkedHashSet<>();
        List<ScheduledSession> created = new ArrayList<>();

        for (String client : clients) {
            if (validation.invalidUsername(client)) continue;

            ScheduledSession s = new ScheduledSession();
            s.setTrainerUsername(trainerUsername);
            s.setClientUsername(client);
            s.setStartTime(req.startTime);
            s.setDurationMinutes(duration);
            s.setNote(note);
            s.setStatus("SCHEDULED");
            s.setCustom(false);
            s.setCreatedAt(System.currentTimeMillis());
            sessionRepo.save(s);
            created.add(s);

            // Push
            pushService.sendToUser(client,
                    "📅 New training session",
                    trainerUsername + " scheduled a session for " + whenStr,
                    "session", trainerUsername);

            // Email (samo ako je verifikovan)
            userRepo.findByUsername(client).ifPresent(u -> {
                if (u.isEmailVerified() && u.getEmail() != null && !u.getEmail().isBlank()) {
                    String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:auto\">"
                            + "<h2 style=\"color:#111\">New training session 📅</h2>"
                            + "<p>Hi <strong>" + client + "</strong>,</p>"
                            + "<p>Your trainer <strong>" + trainerUsername + "</strong> scheduled a session for you:</p>"
                            + "<p style=\"font-size:18px;font-weight:700\">" + whenStr + "</p>"
                            + "<p>Duration: " + duration + " min</p>"
                            + (note.isEmpty() ? "" : "<p>Note: " + note + "</p>")
                            + "<p style=\"color:#888;font-size:12px\">Spite</p></div>";
                    emailService.send(u.getEmail(), "New training session - " + whenStr, html);
                }
            });
        }

        // Custom imena (nisu klijenti aplikacije) — samo zapis, bez notifikacija
        if (req.customNames != null) {
            for (String name : req.customNames) {
                if (name == null) continue;
                String n = name.trim();
                if (n.isEmpty() || n.length() > 60) continue;

                ScheduledSession s = new ScheduledSession();
                s.setTrainerUsername(trainerUsername);
                s.setClientUsername(n);
                s.setStartTime(req.startTime);
                s.setDurationMinutes(duration);
                s.setNote(note);
                s.setStatus("SCHEDULED");
                s.setCustom(true);
                s.setCreatedAt(System.currentTimeMillis());
                sessionRepo.save(s);
                created.add(s);
            }
        }

        // Offline klijenti (sačuvani profili) — šalji mejl na njihovu adresu ako postoji
        if (req.offlineClientIds != null) {
            for (String ocId : req.offlineClientIds) {
                if (ocId == null) continue;
                OfflineClient oc = offlineClientRepo.findById(ocId).orElse(null);
                if (oc == null || !trainerUsername.equals(oc.getTrainerUsername())) continue;

                ScheduledSession s = new ScheduledSession();
                s.setTrainerUsername(trainerUsername);
                s.setClientUsername(oc.getName());
                s.setStartTime(req.startTime);
                s.setDurationMinutes(duration);
                s.setNote(note);
                s.setStatus("SCHEDULED");
                s.setCustom(true);
                s.setOfflineClientId(oc.getId());
                s.setClientEmail(oc.getEmail());
                s.setCreatedAt(System.currentTimeMillis());
                sessionRepo.save(s);
                created.add(s);

                if (EmailService.isValidEmail(oc.getEmail())) {
                    String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:auto\">"
                            + "<h2 style=\"color:#111\">New training session 📅</h2>"
                            + "<p>Hi <strong>" + oc.getName() + "</strong>,</p>"
                            + "<p>Your trainer <strong>" + trainerUsername + "</strong> scheduled a session for you:</p>"
                            + "<p style=\"font-size:18px;font-weight:700\">" + whenStr + "</p>"
                            + "<p>Duration: " + duration + " min</p>"
                            + (note.isEmpty() ? "" : "<p>Note: " + note + "</p>")
                            + "<p style=\"color:#888;font-size:12px\">Spite</p></div>";
                    emailService.send(oc.getEmail(), "New training session - " + whenStr, html);
                }
            }
        }

        return ResponseEntity.ok(created);
    }

    @GetMapping("/client/{clientUsername}")
    public ResponseEntity<?> getClientSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String clientUsername) {

        if (validation.invalidUsername(clientUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        boolean isOwner = actor.equals(clientUsername);
        boolean isTrainer = guard.hasRole(actor, Role.TRAINER);
        if (!isOwner && !isTrainer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        List<ScheduledSession> list = sessionRepo.findByClientUsername(clientUsername);
        list.sort(Comparator.comparingLong(ScheduledSession::getStartTime));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/trainer/{trainerUsername}")
    public ResponseEntity<?> getTrainerSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String trainerUsername) {

        if (validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername)
                || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        List<ScheduledSession> list = sessionRepo.findByTrainerUsername(trainerUsername);
        list.sort(Comparator.comparingLong(ScheduledSession::getStartTime));
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {

        Optional<ScheduledSession> opt = sessionRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Session not found");
        }
        ScheduledSession s = opt.get();

        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null || !actor.equals(s.getTrainerUsername())
                || !guard.hasRole(actor, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        String whenStr = FMT.format(Instant.ofEpochMilli(s.getStartTime()));
        String client = s.getClientUsername();
        sessionRepo.delete(s);

        pushService.sendToUser(client,
                "❌ Session cancelled",
                s.getTrainerUsername() + " cancelled the session on " + whenStr,
                "session", s.getTrainerUsername());

        if (s.getOfflineClientId() != null && EmailService.isValidEmail(s.getClientEmail())) {
            // Offline klijent — mejl ide na adresu sačuvanu uz termin
            String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:auto\">"
                    + "<h2 style=\"color:#111\">Session cancelled</h2>"
                    + "<p>Hi <strong>" + client + "</strong>,</p>"
                    + "<p>Your trainer <strong>" + s.getTrainerUsername()
                    + "</strong> cancelled the session scheduled for <strong>" + whenStr + "</strong>.</p>"
                    + "<p style=\"color:#888;font-size:12px\">Spite</p></div>";
            emailService.send(s.getClientEmail(), "Session cancelled - " + whenStr, html);
        } else {
            userRepo.findByUsername(client).ifPresent(u -> {
                if (u.isEmailVerified() && u.getEmail() != null && !u.getEmail().isBlank()) {
                    String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:auto\">"
                            + "<h2 style=\"color:#111\">Session cancelled</h2>"
                            + "<p>Hi <strong>" + client + "</strong>,</p>"
                            + "<p>Your trainer <strong>" + s.getTrainerUsername()
                            + "</strong> cancelled the session scheduled for <strong>" + whenStr + "</strong>.</p>"
                            + "<p style=\"color:#888;font-size:12px\">Spite</p></div>";
                    emailService.send(u.getEmail(), "Session cancelled - " + whenStr, html);
                }
            });
        }

        return ResponseEntity.ok("Session cancelled");
    }
}
