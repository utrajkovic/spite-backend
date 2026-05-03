package com.spite.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.CompletedWorkout;
import com.spite.backend.model.Workout;
import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.repository.CompletedWorkoutRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class WorkoutFeedbackController {

    private final WorkoutFeedbackRepository repo;
    private final WorkoutRepository workoutRepo;
    private final CompletedWorkoutRepository completedRepo;
    private final TrainerClientRepository trainerClientRepo;
    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final RoleGuardService guard;

    public WorkoutFeedbackController(
            WorkoutFeedbackRepository repo,
            WorkoutRepository workoutRepo,
            CompletedWorkoutRepository completedRepo,
            TrainerClientRepository trainerClientRepo,
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            RoleGuardService guard) {
        this.repo = repo;
        this.workoutRepo = workoutRepo;
        this.completedRepo = completedRepo;
        this.trainerClientRepo = trainerClientRepo;
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.guard = guard;
    }

    private boolean canReadUserFeedback(String authorization, String username) {
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return false;
        }
        if (actor.equals(username)) {
            return true;
        }
        return guard.hasRole(actor, com.spite.backend.model.Role.TRAINER)
                && trainerClientRepo.existsByTrainerUsernameAndClientUsername(actor, username);
    }

    @PostMapping
    public ResponseEntity<?> saveFeedback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody WorkoutFeedback feedback) {
        if (validation.invalidUsername(feedback.getUserId()) || validation.isBlank(feedback.getWorkoutId())) {
            return ResponseEntity.badRequest().body("Invalid feedback payload");
        }
        if (!sessionAuthService.isSameUser(authorization, feedback.getUserId())) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        feedback.setTimestamp(System.currentTimeMillis());

        if (feedback.getWorkoutTitle() == null || feedback.getWorkoutTitle().isBlank()) {
            Optional<Workout> workoutOpt = workoutRepo.findById(feedback.getWorkoutId());
            workoutOpt.ifPresent(w -> feedback.setWorkoutTitle(w.getTitle()));
        }

        WorkoutFeedback saved = repo.save(feedback);

        // Linkuj sa CompletedWorkout ako postoji
        List<CompletedWorkout> completed = completedRepo.findByUsername(feedback.getUserId());
        completed.stream()
            .filter(cw -> cw.getWorkoutId().equals(feedback.getWorkoutId()) && !cw.isHasFeedback())
            .findFirst()
            .ifPresent(cw -> {
                cw.setHasFeedback(true);
                cw.setFeedbackId(saved.getId());
                completedRepo.save(cw);
            });

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFeedback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody WorkoutFeedback updated) {
        return repo.findById(id).map(existing -> {
            if (!sessionAuthService.isSameUser(authorization, existing.getUserId())) {
                return ResponseEntity.status(403).body("Access denied.");
            }
            existing.setExercises(updated.getExercises());
            if (updated.getCompletionPercent() != null) existing.setCompletionPercent(updated.getCompletionPercent());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getFeedbackForUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!canReadUserFeedback(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(repo.findByUserId(username));
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<String> clearFeedbackForUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        repo.deleteByUserId(username);
        completedRepo.deleteByUsername(username);
        return ResponseEntity.ok("Workout history cleared.");
    }
}