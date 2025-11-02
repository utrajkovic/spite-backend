package com.spite.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import com.spite.backend.model.Exercise;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.service.CloudinaryService;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*")
public class ExerciseController {

    private final ExerciseRepository repo;
    private final CloudinaryService cloudinaryService;

    public ExerciseController(ExerciseRepository repo, CloudinaryService cloudinaryService) {
        this.repo = repo;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("video") MultipartFile file) {
        try {
            String videoUrl = cloudinaryService.uploadVideo(file);
            return ResponseEntity.ok(videoUrl);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Exercise> getAll() {
        return repo.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<Exercise> getByUser(@PathVariable String userId) {
        return repo.findByUserId(userId);
    }

    @PostMapping
    public Exercise add(@RequestBody Exercise e) {
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExercise(@PathVariable String id) {
        return repo.findById(id).map(exercise -> {
            if (exercise.getVideoUrl() != null && !exercise.getVideoUrl().isEmpty()) {
                boolean deleted = cloudinaryService.deleteVideo(exercise.getVideoUrl());
                System.out.println(deleted
                        ? "☁️ Cloudinary video deleted: " + exercise.getVideoUrl()
                        : "⚠️ Failed to delete video from Cloudinary");
            }

            repo.deleteById(id);
            return ResponseEntity.ok("Exercise and video deleted.");
        }).orElse(ResponseEntity.notFound().build());
    }

}
