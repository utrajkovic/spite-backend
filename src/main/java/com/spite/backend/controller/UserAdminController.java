package com.spite.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.spite.backend.model.AssignedWorkout;
import com.spite.backend.model.ClientWorkoutLink;
import com.spite.backend.model.Exercise;
import com.spite.backend.model.Role;
import com.spite.backend.model.TrainerClientLink;
import com.spite.backend.model.User;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.AssignedWorkoutRepository;
import com.spite.backend.repository.ClientWorkoutLinkRepository;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.service.CloudinaryService;
import com.spite.backend.service.RoleGuardService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class UserAdminController {

    private final UserRepository repo;
    private final RoleGuardService guard;
    private final ExerciseRepository exerciseRepo;
    private final WorkoutRepository workoutRepo;
    private final CloudinaryService cloudinaryService;
    private final TrainerClientRepository trainerClientRepo;
    private final ClientWorkoutLinkRepository clientWorkoutLinkRepo;
    private final AssignedWorkoutRepository assignedWorkoutRepo;
    private final PasswordEncoder passwordEncoder;

    public UserAdminController(
            UserRepository repo,
            RoleGuardService guard,
            ExerciseRepository exerciseRepo,
            WorkoutRepository workoutRepo,
            CloudinaryService cloudinaryService,
            TrainerClientRepository trainerClientRepo,
            ClientWorkoutLinkRepository clientWorkoutLinkRepo,
            AssignedWorkoutRepository assignedWorkoutRepo,
            PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.guard = guard;
        this.exerciseRepo = exerciseRepo;
        this.workoutRepo = workoutRepo;
        this.cloudinaryService = cloudinaryService;
        this.trainerClientRepo = trainerClientRepo;
        this.clientWorkoutLinkRepo = clientWorkoutLinkRepo;
        this.assignedWorkoutRepo = assignedWorkoutRepo;
        this.passwordEncoder = passwordEncoder;

    }

    @GetMapping
    public List<User> listAll() {
        return repo.findAll();
    }

    @PutMapping("/{username}/role")
    public ResponseEntity<String> updateRole(
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam Role role) {

        if (!guard.hasRole(adminUsername, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can change roles.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = optUser.get();
        user.setRole(role);
        repo.save(user);

        return ResponseEntity.ok("Role updated to " + role);
    }

    @PutMapping("/{username}/password")
    public ResponseEntity<String> updatePassword(
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam String newPassword) {

        if (!guard.hasRole(adminUsername, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can change passwords.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = optUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));

        repo.save(user);

        return ResponseEntity.ok("Password updated successfully.");
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteUser(
            @RequestParam String adminUsername,
            @PathVariable String username) {

        if (!guard.hasRole(adminUsername, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can delete users.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = optUser.get();
        String userId = user.getId();

        List<Exercise> exercises = exerciseRepo.findByUserId(userId);

        for (Exercise ex : exercises) {
            if (ex.getVideoUrl() != null && !ex.getVideoUrl().isEmpty()) {
                cloudinaryService.deleteVideo(ex.getVideoUrl());
            }
        }
        exerciseRepo.deleteAll(exercises);

        List<Workout> workouts = workoutRepo.findByUserId(userId);
        workoutRepo.deleteAll(workouts);

        List<TrainerClientLink> trainerLinks = trainerClientRepo.findByTrainerUsername(username);
        trainerClientRepo.deleteAll(trainerLinks);

        List<TrainerClientLink> clientLinks = trainerClientRepo.findByClientUsername(username);
        trainerClientRepo.deleteAll(clientLinks);

        List<ClientWorkoutLink> cwlClient = clientWorkoutLinkRepo.findByClientUsername(username);
        clientWorkoutLinkRepo.deleteAll(cwlClient);

        List<ClientWorkoutLink> cwlTrainer = clientWorkoutLinkRepo.findAll()
                .stream()
                .filter(link -> username.equals(link.getTrainerUsername()))
                .toList();
        clientWorkoutLinkRepo.deleteAll(cwlTrainer);

        List<AssignedWorkout> assignedClient = assignedWorkoutRepo.findByClientUsername(username);
        assignedWorkoutRepo.deleteAll(assignedClient);

        List<AssignedWorkout> assignedByTrainer = assignedWorkoutRepo
                .findAll().stream()
                .filter(a -> username.equals(a.getAssignedBy()))
                .toList();
        assignedWorkoutRepo.deleteAll(assignedByTrainer);

        repo.delete(user);

        return ResponseEntity.ok("User and all linked data were successfully deleted.");
    }

}
