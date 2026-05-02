package com.spite.backend.controller;

import com.spite.backend.model.CompletedWorkout;
import com.spite.backend.repository.CompletedWorkoutRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/completed-workouts")
@CrossOrigin(origins = "*")
public class CompletedWorkoutController {

    private final CompletedWorkoutRepository repo;

    public CompletedWorkoutController(CompletedWorkoutRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<CompletedWorkout> save(@RequestBody CompletedWorkout cw) {
        cw.setCompletedAt(System.currentTimeMillis());
        return ResponseEntity.ok(repo.save(cw));
    }

    @GetMapping("/user/{username}")
    public List<CompletedWorkout> getByUser(@PathVariable String username) {
        return repo.findByUsername(username);
    }

    @PutMapping("/{id}/feedback")
    public ResponseEntity<?> markFeedback(@PathVariable String id, @RequestParam String feedbackId) {
        return repo.findById(id).map(cw -> {
            cw.setHasFeedback(true);
            cw.setFeedbackId(feedbackId);
            repo.save(cw);
            return ResponseEntity.ok("Updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<String> deleteByUser(@PathVariable String username) {
        repo.deleteByUsername(username);
        return ResponseEntity.ok("Deleted");
    }
}
