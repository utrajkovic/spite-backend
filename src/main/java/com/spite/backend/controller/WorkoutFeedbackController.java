package com.spite.backend.controller;

import com.spite.backend.model.WorkoutFeedback;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class WorkoutFeedbackController {

    private final WorkoutFeedbackRepository repo;

    public WorkoutFeedbackController(WorkoutFeedbackRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public WorkoutFeedback saveFeedback(@RequestBody WorkoutFeedback feedback) {
        feedback.setTimestamp(System.currentTimeMillis());
        return repo.save(feedback);
    }
}
