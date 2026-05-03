package com.spite.backend.controller;

import com.spite.backend.model.Role;
import com.spite.backend.model.VideoComment;
import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.VideoCommentRepository;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video-comments")
@CrossOrigin(origins = "*")
public class VideoCommentController {

    private final VideoCommentRepository commentRepo;
    private final WorkoutFeedbackRepository feedbackRepo;
    private final TrainerClientRepository trainerClientRepo;
    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final RoleGuardService guard;

    public VideoCommentController(
            VideoCommentRepository commentRepo,
            WorkoutFeedbackRepository feedbackRepo,
            TrainerClientRepository trainerClientRepo,
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            RoleGuardService guard) {
        this.commentRepo = commentRepo;
        this.feedbackRepo = feedbackRepo;
        this.trainerClientRepo = trainerClientRepo;
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.guard = guard;
    }

    private boolean canReadFeedback(String authorization, WorkoutFeedback feedback) {
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return false;
        }
        if (actor.equals(feedback.getUserId())) {
            return true;
        }
        return guard.hasRole(actor, Role.TRAINER)
                && trainerClientRepo.existsByTrainerUsernameAndClientUsername(actor, feedback.getUserId());
    }

    @PostMapping
    public ResponseEntity<?> addComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody VideoComment payload) {

        if (validation.isBlank(payload.getFeedbackId())
                || validation.invalidUsername(payload.getTrainerUsername())
                || validation.invalidUsername(payload.getClientUsername())
                || payload.getTimestampSec() == null
                || payload.getTimestampSec() < 0
                || validation.isBlank(payload.getComment())
                || validation.tooLong(payload.getComment(), 500)) {
            return ResponseEntity.badRequest().body("Invalid video comment payload");
        }

        if (!sessionAuthService.isSameUser(authorization, payload.getTrainerUsername())
                || !guard.hasRole(payload.getTrainerUsername(), Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        if (!trainerClientRepo.existsByTrainerUsernameAndClientUsername(payload.getTrainerUsername(), payload.getClientUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Trainer-client link not found.");
        }

        var feedbackOpt = feedbackRepo.findById(payload.getFeedbackId());
        if (feedbackOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Feedback not found");
        }

        if (!payload.getClientUsername().equals(feedbackOpt.get().getUserId())) {
            return ResponseEntity.badRequest().body("Feedback is not owned by specified client");
        }

        payload.setCreatedAt(System.currentTimeMillis());
        return ResponseEntity.ok(commentRepo.save(payload));
    }

    @GetMapping("/feedback/{feedbackId}")
    public ResponseEntity<?> getForFeedback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String feedbackId) {
        if (validation.isBlank(feedbackId)) {
            return ResponseEntity.badRequest().body("Invalid feedback id");
        }

        var feedbackOpt = feedbackRepo.findById(feedbackId);
        if (feedbackOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canReadFeedback(authorization, feedbackOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        return ResponseEntity.ok(commentRepo.findByFeedbackIdOrderByTimestampSecAscCreatedAtAsc(feedbackId));
    }
}
