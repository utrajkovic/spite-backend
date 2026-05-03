package com.spite.backend.controller;

import com.spite.backend.model.DailyCheckIn;
import com.spite.backend.model.Role;
import com.spite.backend.model.TrainerClientLink;
import com.spite.backend.repository.CompletedWorkoutRepository;
import com.spite.backend.repository.DailyCheckInRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.PushNotificationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trainer/inbox")
@CrossOrigin(origins = "*")
public class TrainerInboxController {

    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final RoleGuardService guard;
    private final TrainerClientRepository trainerClientRepo;
    private final DailyCheckInRepository checkInRepo;
    private final WorkoutFeedbackRepository feedbackRepo;
    private final CompletedWorkoutRepository completedRepo;
    private final PushNotificationService pushService;

    public TrainerInboxController(
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            RoleGuardService guard,
            TrainerClientRepository trainerClientRepo,
            DailyCheckInRepository checkInRepo,
            WorkoutFeedbackRepository feedbackRepo,
            CompletedWorkoutRepository completedRepo,
            PushNotificationService pushService) {
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.guard = guard;
        this.trainerClientRepo = trainerClientRepo;
        this.checkInRepo = checkInRepo;
        this.feedbackRepo = feedbackRepo;
        this.completedRepo = completedRepo;
        this.pushService = pushService;
    }

    private boolean canAccessTrainer(String authorization, String trainerUsername) {
        return !validation.invalidUsername(trainerUsername)
                && sessionAuthService.isSameUser(authorization, trainerUsername)
                && guard.hasRole(trainerUsername, Role.TRAINER);
    }

    @GetMapping("/{trainerUsername}")
    public ResponseEntity<?> getInbox(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String trainerUsername) {
        if (!canAccessTrainer(authorization, trainerUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        List<TrainerClientLink> links = trainerClientRepo.findByTrainerUsername(trainerUsername);
        List<String> clients = links.stream().map(TrainerClientLink::getClientUsername).toList();

        List<DailyCheckIn> pendingCheckins = checkInRepo
                .findByTrainerUsernameAndReviewedOrderByCreatedAtDesc(trainerUsername, false);

        List<Map<String, Object>> unreadFeedback = clients.stream().map(client -> {
            long count = feedbackRepo.findByUserId(client).stream()
                    .filter(fb -> fb.getTrainerRead() == null || !fb.getTrainerRead())
                    .count();
            Map<String, Object> row = new HashMap<>();
            row.put("clientUsername", client);
            row.put("unreadCount", count);
            return row;
        }).filter(row -> ((Long) row.get("unreadCount")) > 0).toList();

        List<Map<String, Object>> priorityClients = clients.stream().map(client -> {
            var completed = completedRepo.findByUsername(client);
            long lastCompletedAt = completed.stream()
                    .mapToLong(c -> c.getCompletedAt())
                    .max()
                    .orElse(0L);

            long daysSinceLastWorkout = lastCompletedAt == 0L
                    ? 999L
                    : Duration.between(Instant.ofEpochMilli(lastCompletedAt), Instant.now()).toDays();

            long pendingForClient = pendingCheckins.stream()
                    .filter(c -> client.equals(c.getUsername()))
                    .count();

            long unreadCount = feedbackRepo.findByUserId(client).stream()
                    .filter(fb -> fb.getTrainerRead() == null || !fb.getTrainerRead())
                    .count();

            String risk;
            if (daysSinceLastWorkout >= 5 || pendingForClient >= 2) {
                risk = "HIGH";
            } else if (daysSinceLastWorkout >= 3 || pendingForClient >= 1) {
                risk = "MEDIUM";
            } else {
                risk = "LOW";
            }

            Map<String, Object> row = new HashMap<>();
            row.put("clientUsername", client);
            row.put("daysSinceLastWorkout", daysSinceLastWorkout == 999L ? null : daysSinceLastWorkout);
            row.put("pendingCheckins", pendingForClient);
            row.put("unreadFeedback", unreadCount);
            row.put("risk", risk);
            return row;
        }).sorted(Comparator.comparingInt(x -> {
            String risk = (String) x.get("risk");
            return switch (risk) {
                case "HIGH" -> 0;
                case "MEDIUM" -> 1;
                default -> 2;
            };
        })).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("clients", clients);
        response.put("pendingCheckins", pendingCheckins);
        response.put("unreadFeedback", unreadFeedback);
        response.put("priorityClients", priorityClients);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{trainerUsername}/bulk/remind-late")
    public ResponseEntity<?> remindLateClients(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String trainerUsername) {
        if (!canAccessTrainer(authorization, trainerUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        List<TrainerClientLink> links = trainerClientRepo.findByTrainerUsername(trainerUsername);
        int sent = 0;

        for (TrainerClientLink link : links) {
            String client = link.getClientUsername();
            long lastCompletedAt = completedRepo.findByUsername(client).stream()
                    .mapToLong(c -> c.getCompletedAt())
                    .max()
                    .orElse(0L);

            long daysSinceLastWorkout = lastCompletedAt == 0L
                    ? 999L
                    : Duration.between(Instant.ofEpochMilli(lastCompletedAt), Instant.now()).toDays();

            if (daysSinceLastWorkout >= 3) {
                pushService.sendToUser(
                        client,
                        "📅 Coach reminder",
                        "Your coach sent a reminder. Open Daily Agenda and check in today.",
                        "coach_reminder",
                        trainerUsername
                );
                sent++;
            }
        }

        return ResponseEntity.ok("Reminders sent: " + sent);
    }
}
