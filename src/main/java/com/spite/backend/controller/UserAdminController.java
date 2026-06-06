package com.spite.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.AssignedWorkout;
import com.spite.backend.model.ClientWorkoutLink;
import com.spite.backend.model.Exercise;
import com.spite.backend.model.Role;
import com.spite.backend.model.TrainerClientLink;
import com.spite.backend.model.User;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.AssignedWorkoutRepository;
import com.spite.backend.repository.ClientWorkoutLinkRepository;
import com.spite.backend.repository.CompletedWorkoutRepository;
import com.spite.backend.repository.EmailVerificationTokenRepository;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.MealPlanRepository;
import com.spite.backend.repository.ScheduledSessionRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.repository.WorkoutFeedbackRepository;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.service.CloudinaryService;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

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
    private final CompletedWorkoutRepository completedWorkoutRepo;
    private final WorkoutFeedbackRepository workoutFeedbackRepo;
    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final PasswordEncoder passwordEncoder;
    private final MealPlanRepository mealPlanRepo;
    private final ScheduledSessionRepository scheduledSessionRepo;
    private final EmailVerificationTokenRepository emailTokenRepo;

    public UserAdminController(
            UserRepository repo,
            RoleGuardService guard,
            ExerciseRepository exerciseRepo,
            WorkoutRepository workoutRepo,
            CloudinaryService cloudinaryService,
            TrainerClientRepository trainerClientRepo,
            ClientWorkoutLinkRepository clientWorkoutLinkRepo,
            AssignedWorkoutRepository assignedWorkoutRepo,
            CompletedWorkoutRepository completedWorkoutRepo,
            WorkoutFeedbackRepository workoutFeedbackRepo,
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            PasswordEncoder passwordEncoder,
            MealPlanRepository mealPlanRepo,
            ScheduledSessionRepository scheduledSessionRepo,
            EmailVerificationTokenRepository emailTokenRepo) {
        this.repo = repo;
        this.guard = guard;
        this.exerciseRepo = exerciseRepo;
        this.workoutRepo = workoutRepo;
        this.cloudinaryService = cloudinaryService;
        this.trainerClientRepo = trainerClientRepo;
        this.clientWorkoutLinkRepo = clientWorkoutLinkRepo;
        this.assignedWorkoutRepo = assignedWorkoutRepo;
        this.completedWorkoutRepo = completedWorkoutRepo;
        this.workoutFeedbackRepo = workoutFeedbackRepo;
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.passwordEncoder = passwordEncoder;
        this.mealPlanRepo = mealPlanRepo;
        this.scheduledSessionRepo = scheduledSessionRepo;
        this.emailTokenRepo = emailTokenRepo;
    }

    private boolean isAuthorizedAdmin(String authorization, String adminUsername) {
        if (validation.invalidUsername(adminUsername)) {
            return false;
        }
        return sessionAuthService.isSameUser(authorization, adminUsername)
                && guard.hasRole(adminUsername, Role.ADMIN);
    }

    @GetMapping
    public ResponseEntity<?> listAll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String adminUsername) {
        if (!isAuthorizedAdmin(authorization, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        return ResponseEntity.ok(repo.findAll());
    }

    @PutMapping("/{username}/role")
    public ResponseEntity<String> updateRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam Role role) {

        if (!isAuthorizedAdmin(authorization, adminUsername)) {
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
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam String newPassword) {

        if (!isAuthorizedAdmin(authorization, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can change passwords.");
        }

        if (validation.invalidPassword(newPassword)) {
            return ResponseEntity.badRequest().body("Invalid password format");
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

    @PutMapping("/{username}/block")
    public ResponseEntity<String> blockUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String adminUsername,
            @PathVariable String username) {

        if (!isAuthorizedAdmin(authorization, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) return ResponseEntity.badRequest().body("User not found");

        User user = optUser.get();
        user.setBlocked(true);
        repo.save(user);
        sessionAuthService.invalidateByUsername(user.getUsername());
        return ResponseEntity.ok("User blocked");
    }

    @PutMapping("/{username}/unblock")
    public ResponseEntity<String> unblockUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String adminUsername,
            @PathVariable String username) {

        if (!isAuthorizedAdmin(authorization, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) return ResponseEntity.badRequest().body("User not found");

        User user = optUser.get();
        user.setBlocked(false);
        repo.save(user);
        return ResponseEntity.ok("User unblocked");
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String adminUsername,
            @PathVariable String username) {

        if (!isAuthorizedAdmin(authorization, adminUsername)) {
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

        completedWorkoutRepo.deleteByUsername(username);
        workoutFeedbackRepo.deleteByUserId(username);
        mealPlanRepo.deleteByClientUsername(username);
        scheduledSessionRepo.deleteByClientUsername(username);
        scheduledSessionRepo.deleteByTrainerUsername(username);
        emailTokenRepo.deleteByUsername(username);
        sessionAuthService.invalidateByUsername(username);

        repo.delete(user);

        return ResponseEntity.ok("User and all linked data were successfully deleted.");
    }
}
