package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

import com.spite.backend.model.Workout;
import com.spite.backend.model.Exercise;
import com.spite.backend.model.AssignedWorkout;
import com.spite.backend.dto.WorkoutWithExercises;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.AssignedWorkoutRepository;
import com.spite.backend.service.PushNotificationService;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutRepository workoutRepo;
    private final ExerciseRepository exerciseRepo;
    private final AssignedWorkoutRepository assignedRepo;
    private final PushNotificationService pushService;

    public WorkoutController(
            WorkoutRepository workoutRepo,
            ExerciseRepository exerciseRepo,
            AssignedWorkoutRepository assignedRepo,
            PushNotificationService pushService) {
        this.workoutRepo = workoutRepo;
        this.exerciseRepo = exerciseRepo;
        this.assignedRepo = assignedRepo;
        this.pushService = pushService;
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
    public Workout add(@RequestBody Workout w) {
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
        return workoutRepo.save(w);
    }

    @DeleteMapping("/{wid}")
    public void delete(@PathVariable String wid) {
        workoutRepo.deleteById(wid);
    }

    @GetMapping("/user/{userId}")
    public List<Workout> getByUser(@PathVariable String userId) {
        List<Workout> workouts = workoutRepo.findByUserId(userId);
        workouts.forEach(this::hydrateExercises);
        return workouts;
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignWorkoutToClient(
            @RequestParam String workoutId,
            @RequestParam String clientUsername,
            @RequestParam String assignedBy) {
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
            @RequestParam String workoutId,
            @RequestParam String clientUsername) {
        var linkOpt = assignedRepo.findByClientUsernameAndWorkoutId(clientUsername, workoutId);
        if (linkOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Assignment not found");
        }
        assignedRepo.delete(linkOpt.get());
        return ResponseEntity.ok("Unassigned successfully");
    }

    @PutMapping("/assign/note")
    public ResponseEntity<?> updateNote(
            @RequestParam String workoutId,
            @RequestParam String clientUsername,
            @RequestParam(required = false) String note) {
        var linkOpt = assignedRepo.findByClientUsernameAndWorkoutId(clientUsername, workoutId);
        if (linkOpt.isEmpty()) return ResponseEntity.badRequest().body("Assignment not found");
        AssignedWorkout link = linkOpt.get();
        link.setNote(note != null ? note : "");
        assignedRepo.save(link);
        return ResponseEntity.ok("Note updated");
    }

    @GetMapping("/client/{username}")
    public ResponseEntity<?> getClientWorkoutsWithExercises(@PathVariable String username) {
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
                    exercises);
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{wid}")
    public ResponseEntity<?> updateWorkout(
            @PathVariable String wid,
            @RequestBody Workout updated) {

        var opt = workoutRepo.findById(wid);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Workout not found");
        }

        Workout existing = opt.get();

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
