package com.spite.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.OfflineClient;
import com.spite.backend.model.OfflineClientLog;
import com.spite.backend.model.Role;
import com.spite.backend.repository.OfflineClientLogRepository;
import com.spite.backend.repository.OfflineClientRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/offline-clients")
@CrossOrigin(origins = "*")
public class OfflineClientController {

    private final OfflineClientRepository clientRepo;
    private final OfflineClientLogRepository logRepo;
    private final SessionAuthService sessionAuthService;
    private final RoleGuardService guard;
    private final InputValidationService validation;

    public OfflineClientController(
            OfflineClientRepository clientRepo,
            OfflineClientLogRepository logRepo,
            SessionAuthService sessionAuthService,
            RoleGuardService guard,
            InputValidationService validation) {
        this.clientRepo = clientRepo;
        this.logRepo = logRepo;
        this.sessionAuthService = sessionAuthService;
        this.guard = guard;
        this.validation = validation;
    }

    /** Vraća username trenera ako je autorizovan i ima TRAINER ulogu, inače null. */
    private String authedTrainer(String authorization) {
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null || !guard.hasRole(actor, Role.TRAINER)) return null;
        return actor;
    }

    private void copyProfile(OfflineClient target, OfflineClient src) {
        if (src.getName() != null) target.setName(src.getName().trim());
        target.setEmail(src.getEmail() == null ? null : src.getEmail().trim());
        target.setHeightCm(src.getHeightCm());
        target.setWeightKg(src.getWeightKg());
        target.setGoal(src.getGoal() == null ? null : src.getGoal().trim());
        target.setNotes(src.getNotes());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String trainerUsername,
            @RequestBody OfflineClient body) {

        if (validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid trainer username");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername)
                || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        if (body == null || body.getName() == null || body.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }

        OfflineClient c = new OfflineClient();
        c.setTrainerUsername(trainerUsername);
        copyProfile(c, body);
        c.setCreatedAt(System.currentTimeMillis());
        clientRepo.save(c);
        return ResponseEntity.ok(c);
    }

    @GetMapping("/trainer/{trainerUsername}")
    public ResponseEntity<?> listForTrainer(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String trainerUsername) {

        if (validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid trainer username");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername)
                || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        return ResponseEntity.ok(clientRepo.findByTrainerUsername(trainerUsername));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {

        String trainer = authedTrainer(authorization);
        if (trainer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        Optional<OfflineClient> opt = clientRepo.findById(id);
        if (opt.isEmpty() || !trainer.equals(opt.get().getTrainerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        return ResponseEntity.ok(opt.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody OfflineClient body) {

        String trainer = authedTrainer(authorization);
        if (trainer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        Optional<OfflineClient> opt = clientRepo.findById(id);
        if (opt.isEmpty() || !trainer.equals(opt.get().getTrainerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        if (body == null || body.getName() == null || body.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }

        OfflineClient c = opt.get();
        copyProfile(c, body);
        clientRepo.save(c);
        return ResponseEntity.ok(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {

        String trainer = authedTrainer(authorization);
        if (trainer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        Optional<OfflineClient> opt = clientRepo.findById(id);
        if (opt.isEmpty() || !trainer.equals(opt.get().getTrainerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        logRepo.deleteByOfflineClientId(id);
        clientRepo.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // ───────── Training log ─────────

    @GetMapping("/{id}/logs")
    public ResponseEntity<?> listLogs(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {

        String trainer = authedTrainer(authorization);
        if (trainer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        Optional<OfflineClient> opt = clientRepo.findById(id);
        if (opt.isEmpty() || !trainer.equals(opt.get().getTrainerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        return ResponseEntity.ok(logRepo.findByOfflineClientIdOrderByDateDesc(id));
    }

    @PostMapping("/{id}/logs")
    public ResponseEntity<?> addLog(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody OfflineClientLog body) {

        String trainer = authedTrainer(authorization);
        if (trainer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        Optional<OfflineClient> opt = clientRepo.findById(id);
        if (opt.isEmpty() || !trainer.equals(opt.get().getTrainerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        if (body == null) {
            return ResponseEntity.badRequest().body("Missing body");
        }

        OfflineClientLog log = new OfflineClientLog();
        log.setOfflineClientId(id);
        log.setTrainerUsername(trainer);
        log.setDate(body.getDate() > 0 ? body.getDate() : System.currentTimeMillis());
        log.setWorkoutTitle(body.getWorkoutTitle() == null ? "" : body.getWorkoutTitle().trim());
        log.setNote(body.getNote());
        log.setBodyWeightKg(body.getBodyWeightKg());
        if (body.getExercises() != null) log.setExercises(body.getExercises());
        if (body.getRecords() != null) log.setRecords(body.getRecords());
        log.setCreatedAt(System.currentTimeMillis());
        logRepo.save(log);

        // Ako log nosi telesnu kilažu, ažuriraj trenutnu kilažu klijenta
        if (body.getBodyWeightKg() != null) {
            OfflineClient c = opt.get();
            c.setWeightKg(body.getBodyWeightKg());
            clientRepo.save(c);
        }

        return ResponseEntity.ok(log);
    }

    @DeleteMapping("/logs/{logId}")
    public ResponseEntity<?> deleteLog(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String logId) {

        String trainer = authedTrainer(authorization);
        if (trainer == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        Optional<OfflineClientLog> opt = logRepo.findById(logId);
        if (opt.isEmpty() || !trainer.equals(opt.get().getTrainerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        logRepo.deleteById(logId);
        return ResponseEntity.ok("Deleted");
    }
}
