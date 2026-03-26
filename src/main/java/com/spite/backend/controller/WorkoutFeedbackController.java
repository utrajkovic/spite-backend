package com.spite.backend.controller;

import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.repository.WorkoutRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

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

        // Snapshot workoutTitle ako nije prosleđen
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
}
