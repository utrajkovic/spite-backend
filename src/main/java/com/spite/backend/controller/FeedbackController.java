package com.spite.backend.controller;

import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.repository.WorkoutRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private final WorkoutFeedbackRepository feedbackRepo;
    private final WorkoutRepository workoutRepo;

    public FeedbackController(WorkoutFeedbackRepository feedbackRepo, WorkoutRepository workoutRepo) {
        this.feedbackRepo = feedbackRepo;
        this.workoutRepo = workoutRepo;
    }

    /**
     * Čuva feedback. Snapshot-uje workoutTitle u trenutku slanja
     * kako bi ostao čitljiv čak i ako se trening obriše.
     */
    @PostMapping
    public ResponseEntity<?> saveFeedback(@RequestBody WorkoutFeedback feedback) {
        // Ako workoutTitle nije prosleđen, pokušaj da ga dohvatiš
        if (feedback.getWorkoutTitle() == null || feedback.getWorkoutTitle().isBlank()) {
            Optional<Workout> workoutOpt = workoutRepo.findById(feedback.getWorkoutId());
            workoutOpt.ifPresent(w -> feedback.setWorkoutTitle(w.getTitle()));
        }

        WorkoutFeedback saved = feedbackRepo.save(feedback);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkoutFeedback>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(feedbackRepo.findByUserId(userId));
    }
}
