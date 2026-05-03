package com.spite.backend.controller;

import com.spite.backend.model.CompletedWorkout;
import com.spite.backend.repository.CompletedWorkoutRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.SessionAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/completed-workouts")
@CrossOrigin(origins = "*")
public class CompletedWorkoutController {

    private final CompletedWorkoutRepository repo;
    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;

    public CompletedWorkoutController(
            CompletedWorkoutRepository repo,
            SessionAuthService sessionAuthService,
            InputValidationService validation) {
        this.repo = repo;
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
    }

    @PostMapping
    public ResponseEntity<?> save(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CompletedWorkout cw) {
        if (validation.invalidUsername(cw.getUsername()) || validation.isBlank(cw.getWorkoutId())) {
            return ResponseEntity.badRequest().body("Invalid completed workout payload");
        }
        if (!sessionAuthService.isSameUser(authorization, cw.getUsername())) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        cw.setCompletedAt(System.currentTimeMillis());
        return ResponseEntity.ok(repo.save(cw));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getByUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        return ResponseEntity.ok(repo.findByUsername(username));
    }

    @PutMapping("/{id}/feedback")
    public ResponseEntity<?> markFeedback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestParam String feedbackId) {
        return repo.findById(id).map(cw -> {
            if (!sessionAuthService.isSameUser(authorization, cw.getUsername())) {
                return ResponseEntity.status(403).body("Access denied.");
            }
            if (validation.isBlank(feedbackId)) {
                return ResponseEntity.badRequest().body("Invalid feedback id");
            }
            cw.setHasFeedback(true);
            cw.setFeedbackId(feedbackId);
            repo.save(cw);
            return ResponseEntity.ok("Updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<String> deleteByUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        repo.deleteByUsername(username);
        return ResponseEntity.ok("Deleted");
    }
}
