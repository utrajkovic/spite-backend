package com.spite.backend.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.spite.backend.model.Exercise;
import com.spite.backend.model.Role;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.BunnyCDNService;
import com.spite.backend.service.CloudinaryService;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*")
public class ExerciseController {

    private final ExerciseRepository repo;
    private final CloudinaryService cloudinaryService;
    private final BunnyCDNService bunnyCDNService;
    private final SessionAuthService sessionAuthService;
    private final UserRepository userRepo;
    private final InputValidationService validation;
    private final RoleGuardService guard;

    public ExerciseController(
            ExerciseRepository repo,
            CloudinaryService cloudinaryService,
            BunnyCDNService bunnyCDNService,
            SessionAuthService sessionAuthService,
            UserRepository userRepo,
            InputValidationService validation,
            RoleGuardService guard) {
        this.repo = repo;
        this.cloudinaryService = cloudinaryService;
        this.bunnyCDNService = bunnyCDNService;
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

            String videoUrl = bunnyCDNService.uploadVideo(file);

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
                boolean deleted = bunnyCDNService.deleteVideo(exercise.getVideoUrl());
                System.out.println(deleted
                        ? "✅ BunnyCDN video deleted: " + exercise.getVideoUrl()
                        : "⚠️ Failed to delete video from BunnyCDN");
            }

            repo.deleteById(id);
            return ResponseEntity.ok("Exercise and video deleted.");
        }).orElse(ResponseEntity.notFound().build());
    }
}
