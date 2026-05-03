package com.spite.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.cloudinary.Transformation;

import com.spite.backend.model.Exercise;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.service.CloudinaryService;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.model.Role;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*")
public class ExerciseController {

    private final ExerciseRepository repo;
    private final CloudinaryService cloudinaryService;
    private final SessionAuthService sessionAuthService;
    private final UserRepository userRepo;
    private final InputValidationService validation;
    private final RoleGuardService guard;

    public ExerciseController(
            ExerciseRepository repo,
            CloudinaryService cloudinaryService,
            SessionAuthService sessionAuthService,
            UserRepository userRepo,
            InputValidationService validation,
            RoleGuardService guard) {
        this.repo = repo;
        this.cloudinaryService = cloudinaryService;
        this.sessionAuthService = sessionAuthService;
        this.userRepo = userRepo;
        this.validation = validation;
        this.guard = guard;
    }

    private boolean canMutateExercise(String authorization, Exercise exercise) {
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return false;
        }
        if (guard.hasRole(actor, Role.ADMIN)) {
            return true;
        }
        var actorUser = userRepo.findByUsername(actor).orElse(null);
        return actorUser != null && exercise.getUserId() != null && exercise.getUserId().equals(actorUser.getId());
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("video") MultipartFile file) {
        try {
            if (sessionAuthService.getUsername(authorization).isEmpty()) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No video file uploaded.");
            }

            Map<String, Object> uploadOptions = new LinkedHashMap<>();
            uploadOptions.put("resource_type", "video");
            uploadOptions.put("format", "mp4");
            uploadOptions.put("transformation", new Transformation<>()
                    .videoCodec("h264")
                    .bitRate("800k")
                    .width(720)
                    .crop("limit")
                    .duration("5"));

            String videoUrl = cloudinaryService.upload(file, uploadOptions);

            return ResponseEntity.ok(videoUrl);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
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
    public ResponseEntity<?> add(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Exercise e) {
        if (validation.isBlank(e.getName()) || validation.tooLong(e.getName(), 120)) {
            return ResponseEntity.badRequest().body("Invalid exercise name");
        }
        if (validation.isBlank(e.getUserId())) {
            return ResponseEntity.badRequest().body("Missing exercise owner");
        }

        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        var actorUser = userRepo.findByUsername(actor).orElse(null);
        if (actorUser == null || (!guard.hasRole(actor, Role.ADMIN) && !e.getUserId().equals(actorUser.getId()))) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        return ResponseEntity.ok(repo.save(e));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExercise(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        return repo.findById(id).map(exercise -> {
            if (!canMutateExercise(authorization, exercise)) {
                return ResponseEntity.status(403).body("Access denied.");
            }

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
