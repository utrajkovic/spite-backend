package com.spite.backend.controller;

import com.spite.backend.model.DailyCheckIn;
import com.spite.backend.model.Role;
import com.spite.backend.repository.DailyCheckInRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/checkins")
@CrossOrigin(origins = "*")
public class CheckInController {

    private final DailyCheckInRepository checkInRepo;
    private final TrainerClientRepository trainerClientRepo;
    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final RoleGuardService guard;

    public CheckInController(
            DailyCheckInRepository checkInRepo,
            TrainerClientRepository trainerClientRepo,
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            RoleGuardService guard) {
        this.checkInRepo = checkInRepo;
        this.trainerClientRepo = trainerClientRepo;
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.guard = guard;
    }

    private String todayKey() {
        return LocalDate.now(ZoneId.systemDefault()).toString();
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DailyCheckIn payload) {

        if (validation.invalidUsername(payload.getUsername()) || validation.invalidUsername(payload.getTrainerUsername())) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, payload.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        if (!trainerClientRepo.existsByTrainerUsernameAndClientUsername(payload.getTrainerUsername(), payload.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Trainer-client link not found.");
        }
        if (payload.getSleepHours() == null || payload.getSleepHours() < 0 || payload.getSleepHours() > 24) {
            return ResponseEntity.badRequest().body("Sleep must be between 0 and 24");
        }
        if (payload.getEnergy() == null || payload.getEnergy() < 1 || payload.getEnergy() > 5) {
            return ResponseEntity.badRequest().body("Energy must be between 1 and 5");
        }
        if (payload.getPain() == null || payload.getPain() < 1 || payload.getPain() > 5) {
            return ResponseEntity.badRequest().body("Pain must be between 1 and 5");
        }
        if (payload.getWeight() != null && (payload.getWeight() <= 0 || payload.getWeight() > 500)) {
            return ResponseEntity.badRequest().body("Weight is out of range");
        }
        if (validation.tooLong(payload.getComment(), 1000)) {
            return ResponseEntity.badRequest().body("Comment is too long");
        }

        String dateKey = todayKey();
        if (checkInRepo.existsByUsernameAndDateKey(payload.getUsername(), dateKey)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Check-in for today already exists");
        }

        payload.setDateKey(dateKey);
        payload.setCreatedAt(System.currentTimeMillis());
        payload.setReviewed(false);

        return ResponseEntity.ok(checkInRepo.save(payload));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getForUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        return ResponseEntity.ok(checkInRepo.findByUsernameOrderByCreatedAtDesc(username));
    }

    @GetMapping("/trainer/{trainerUsername}")
    public ResponseEntity<?> getForTrainer(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String trainerUsername,
            @RequestParam(defaultValue = "true") boolean pendingOnly) {
        if (validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername) || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        List<DailyCheckIn> result = pendingOnly
                ? checkInRepo.findByTrainerUsernameAndReviewedOrderByCreatedAtDesc(trainerUsername, false)
                : checkInRepo.findByTrainerUsernameOrderByCreatedAtDesc(trainerUsername);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<?> markReviewed(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        return checkInRepo.findById(id).map(checkIn -> {
            if (!sessionAuthService.isSameUser(authorization, checkIn.getTrainerUsername())
                    || !guard.hasRole(checkIn.getTrainerUsername(), Role.TRAINER)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
            }
            checkIn.setReviewed(true);
            checkInRepo.save(checkIn);
            return ResponseEntity.ok("Marked as reviewed");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/trainer/{trainerUsername}/review-all")
    public ResponseEntity<?> markAllReviewed(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String trainerUsername) {
        if (validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername) || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        var list = checkInRepo.findByTrainerUsernameAndReviewedOrderByCreatedAtDesc(trainerUsername, false);
        list.forEach(item -> item.setReviewed(true));
        checkInRepo.saveAll(list);
        return ResponseEntity.ok("Marked " + list.size() + " check-ins as reviewed");
    }
}
