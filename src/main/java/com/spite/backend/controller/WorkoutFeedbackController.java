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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.CompletedWorkout;
import com.spite.backend.model.Workout;
import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.repository.CompletedWorkoutRepository;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.repository.WorkoutRepository;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class WorkoutFeedbackController {

    private final WorkoutFeedbackRepository repo;
    private final WorkoutRepository workoutRepo;
    private final CompletedWorkoutRepository completedRepo;

    public WorkoutFeedbackController(WorkoutFeedbackRepository repo, WorkoutRepository workoutRepo, CompletedWorkoutRepository completedRepo) {
        this.repo = repo;
        this.workoutRepo = workoutRepo;
        this.completedRepo = completedRepo;
    }

    @PostMapping
    public WorkoutFeedback saveFeedback(@RequestBody WorkoutFeedback feedback) {
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

        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutFeedback> updateFeedback(@PathVariable String id, @RequestBody WorkoutFeedback updated) {
        return repo.findById(id).map(existing -> {
            existing.setExercises(updated.getExercises());
            if (updated.getCompletionPercent() != null) existing.setCompletionPercent(updated.getCompletionPercent());
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{username}")
    public List<WorkoutFeedback> getFeedbackForUser(@PathVariable String username) {
        return repo.findByUserId(username);
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<String> clearFeedbackForUser(@PathVariable String username) {
        List<WorkoutFeedback> feedbacks = repo.findByUserId(username);
        repo.deleteAll(feedbacks);
        return ResponseEntity.ok("Workout history cleared.");
    }
}