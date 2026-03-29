package com.spite.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.Workout;
import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.repository.WorkoutRepository;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class WorkoutFeedbackController {

    private final WorkoutFeedbackRepository repo;
    private final WorkoutRepository workoutRepo;

    public WorkoutFeedbackController(WorkoutFeedbackRepository repo, WorkoutRepository workoutRepo) {
        this.repo = repo;
        this.workoutRepo = workoutRepo;
    }

    @PostMapping
    public WorkoutFeedback saveFeedback(@RequestBody WorkoutFeedback feedback) {
        feedback.setTimestamp(System.currentTimeMillis());

        if (feedback.getWorkoutTitle() == null || feedback.getWorkoutTitle().isBlank()) {
            Optional<Workout> workoutOpt = workoutRepo.findById(feedback.getWorkoutId());
            workoutOpt.ifPresent(w -> feedback.setWorkoutTitle(w.getTitle()));
        }

        return repo.save(feedback);
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