package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

import com.spite.backend.model.Workout;
import com.spite.backend.model.Exercise;
import com.spite.backend.model.AssignedWorkout;
import com.spite.backend.dto.WorkoutWithExercises;
import com.spite.backend.model.Role;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.AssignedWorkoutRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.PushNotificationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutRepository workoutRepo;
    private final ExerciseRepository exerciseRepo;
    private final AssignedWorkoutRepository assignedRepo;
    private final PushNotificationService pushService;
    private final UserRepository userRepo;
    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final RoleGuardService guard;
    private final TrainerClientRepository trainerClientRepo;

    public WorkoutController(
            WorkoutRepository workoutRepo,
            ExerciseRepository exerciseRepo,
            AssignedWorkoutRepository assignedRepo,
            PushNotificationService pushService,
            UserRepository userRepo,
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            RoleGuardService guard,
            TrainerClientRepository trainerClientRepo) {
        this.workoutRepo = workoutRepo;
        this.exerciseRepo = exerciseRepo;
        this.assignedRepo = assignedRepo;
        this.pushService = pushService;
        this.userRepo = userRepo;
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.guard = guard;
        this.trainerClientRepo = trainerClientRepo;
    }

    private boolean canAccessUserData(String authorization, String username) {
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return false;
        }
        return actor.equals(username)
                || guard.hasRole(actor, Role.ADMIN)
                || trainerClientRepo.existsByTrainerUsernameAndClientUsername(actor, username);
    }

    private boolean canMutateWorkout(String authorization, Workout workout) {
        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return false;
        }
        if (guard.hasRole(actor, Role.ADMIN)) {
            return true;
        }
        var actorUser = userRepo.findByUsername(actor).orElse(null);
        if (actorUser == null) {
            return false;
        }
        return workout.getUserId() != null && workout.getUserId().equals(actorUser.getId());
    }

    private List<String> resolveExerciseIds(Workout w) {
        if (w.getItems() != null && !w.getItems().isEmpty()) {
            return w.getItems().stream()
                    .flatMap(item -> java.util.stream.Stream.of(
                            item.getExerciseId(),
                            item.getSupersetExerciseId()))
                    .filter(eid -> eid != null && !eid.isBlank())
                    .distinct()
                    .toList();
        }
        return w.getExerciseIds();
    }

    private void hydrateExercises(Workout w) {
        List<String> ids = resolveExerciseIds(w);
        if (ids == null || ids.isEmpty()) {
            w.setExercises(List.of());
            return;
        }
        List<Exercise> exList = exerciseRepo.findAllById(ids);
        w.setExercises(exList);
    }

    @GetMapping
    public List<Workout> getAll(@RequestParam(required = false) String userId) {
        List<Workout> workouts = (userId != null && !userId.isEmpty())
                ? workoutRepo.findByUserId(userId)
                : workoutRepo.findAll();
        workouts.forEach(this::hydrateExercises);
        return workouts;
    }

    @GetMapping("/{wid}")
    public Workout getById(@PathVariable String wid) {
        Workout w = workoutRepo.findById(wid).orElse(null);
        if (w != null) hydrateExercises(w);
        return w;
    }

    @PostMapping
    public ResponseEntity<?> add(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Workout w) {
        if (validation.isBlank(w.getTitle()) || validation.tooLong(w.getTitle(), 120)) {
            return ResponseEntity.badRequest().body("Invalid workout title");
        }
        if (validation.isBlank(w.getUserId())) {
            return ResponseEntity.badRequest().body("Missing workout owner");
        }

        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        var actorUser = userRepo.findByUsername(actor).orElse(null);
        if (actorUser == null || (!guard.hasRole(actor, Role.ADMIN) && !w.getUserId().equals(actorUser.getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        if (w.getItems() != null && !w.getItems().isEmpty()) {
            List<String> ids = w.getItems().stream()
                    .flatMap(item -> java.util.stream.Stream.of(
                            item.getExerciseId(),
                            item.getSupersetExerciseId()))
                    .filter(eid -> eid != null && !eid.isBlank())
                    .distinct()
                    .toList();
            w.setExerciseIds(ids);
        }
        return ResponseEntity.ok(workoutRepo.save(w));
    }

    @DeleteMapping("/{wid}")
    public ResponseEntity<?> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String wid) {
        Workout existing = workoutRepo.findById(wid).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canMutateWorkout(authorization, existing)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        workoutRepo.deleteById(wid);
        return ResponseEntity.ok("Workout deleted");
    }

    @GetMapping("/user/{userId}")
    public List<Workout> getByUser(@PathVariable String userId) {
        List<Workout> workouts = workoutRepo.findByUserId(userId);
        workouts.forEach(this::hydrateExercises);
        return workouts;
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignWorkoutToClient(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String workoutId,
            @RequestParam String clientUsername,
            @RequestParam String assignedBy) {
        if (validation.isBlank(workoutId) || validation.invalidUsername(clientUsername) || validation.invalidUsername(assignedBy)) {
            return ResponseEntity.badRequest().body("Invalid assign request");
        }
        if (!sessionAuthService.isSameUser(authorization, assignedBy) || !guard.hasRole(assignedBy, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        var workoutOpt = workoutRepo.findById(workoutId);
        if (workoutOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Workout not found");
        }
        var existing = assignedRepo.findByClientUsernameAndWorkoutId(clientUsername, workoutId);
        if (existing.isPresent()) {
            return ResponseEntity.ok("Already assigned");
        }
        assignedRepo.save(new AssignedWorkout(workoutId, clientUsername, assignedBy));
        // Push notifikacija klijentu
        var workout = workoutOpt.get();
        pushService.sendToUser(clientUsername,
                "🏋️ New workout assigned",
                "Your trainer assigned you: " + workout.getTitle(),
                "workout_assigned", workoutId);
        return ResponseEntity.ok("Assigned successfully");
    }

    @DeleteMapping("/assign")
    public ResponseEntity<?> unassignWorkoutFromClient(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String workoutId,
            @RequestParam String clientUsername,
            @RequestParam String assignedBy) {
        if (validation.isBlank(workoutId) || validation.invalidUsername(clientUsername) || validation.invalidUsername(assignedBy)) {
            return ResponseEntity.badRequest().body("Invalid unassign request");
        }
        if (!sessionAuthService.isSameUser(authorization, assignedBy) || !guard.hasRole(assignedBy, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        var linkOpt = assignedRepo.findByClientUsernameAndWorkoutId(clientUsername, workoutId);
        if (linkOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Assignment not found");
        }
        assignedRepo.delete(linkOpt.get());
        return ResponseEntity.ok("Unassigned successfully");
    }

    @PutMapping("/assign/note")
    public ResponseEntity<?> updateNote(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String workoutId,
            @RequestParam String clientUsername,
            @RequestParam String trainerUsername,
            @RequestParam(required = false) String note) {
        if (validation.isBlank(workoutId) || validation.invalidUsername(clientUsername) || validation.invalidUsername(trainerUsername)) {
            return ResponseEntity.badRequest().body("Invalid note request");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername) || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        if (validation.tooLong(note, 1000)) {
            return ResponseEntity.badRequest().body("Note is too long");
        }

        var linkOpt = assignedRepo.findByClientUsernameAndWorkoutId(clientUsername, workoutId);
        if (linkOpt.isEmpty()) return ResponseEntity.badRequest().body("Assignment not found");
        AssignedWorkout link = linkOpt.get();
        link.setNote(note != null ? note : "");
        assignedRepo.save(link);
        return ResponseEntity.ok("Note updated");
    }

    @GetMapping("/client/{username}")
    public ResponseEntity<?> getClientWorkoutsWithExercises(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!canAccessUserData(authorization, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        var links = assignedRepo.findByClientUsername(username);
        var workoutIds = links.stream().map(AssignedWorkout::getWorkoutId).toList();
        var workouts = workoutRepo.findAllById(workoutIds);

        var result = workouts.stream().map(w -> {
            var ids = resolveExerciseIds(w);
            var exercises = (ids == null || ids.isEmpty())
                    ? List.<Exercise>of()
                    : ids.stream()
                            .map(exerciseRepo::findById)
                            .filter(java.util.Optional::isPresent)
                            .map(java.util.Optional::get)
                            .toList();

            // Nađi napomenu iz assigned linka
            String note = links.stream()
                    .filter(l -> l.getWorkoutId().equals(w.getId()))
                    .map(AssignedWorkout::getNote)
                    .filter(n -> n != null && !n.isBlank())
                    .findFirst()
                    .orElse(null);

            return new WorkoutWithExercises(
                    w.getId(),
                    w.getTitle(),
                    w.getSubtitle(),
                    w.getContent(),
                    note,
                    exercises,
                    w.getItems());
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{wid}")
    public ResponseEntity<?> updateWorkout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String wid,
            @RequestBody Workout updated) {

        var opt = workoutRepo.findById(wid);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Workout not found");
        }

        Workout existing = opt.get();
        if (!canMutateWorkout(authorization, existing)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        if (updated.getTitle() != null && (validation.isBlank(updated.getTitle()) || validation.tooLong(updated.getTitle(), 120))) {
            return ResponseEntity.badRequest().body("Invalid workout title");
        }
        if (updated.getContent() != null && validation.tooLong(updated.getContent(), 4000)) {
            return ResponseEntity.badRequest().body("Workout content is too long");
        }

        if (updated.getTitle() != null) existing.setTitle(updated.getTitle());
        if (updated.getSubtitle() != null) existing.setSubtitle(updated.getSubtitle());
        if (updated.getContent() != null) existing.setContent(updated.getContent());

        if (updated.getItems() != null) {
            existing.setItems(updated.getItems());
            List<String> ids = updated.getItems().stream()
                    .flatMap(item -> java.util.stream.Stream.of(
                            item.getExerciseId(),
                            item.getSupersetExerciseId()))
                    .filter(eid -> eid != null && !eid.isBlank())
                    .distinct()
                    .toList();
            existing.setExerciseIds(ids);
        } else if (updated.getExerciseIds() != null) {
            existing.setExerciseIds(updated.getExerciseIds());
        }

        workoutRepo.save(existing);
        hydrateExercises(existing);

        return ResponseEntity.ok(existing);
    }
}
