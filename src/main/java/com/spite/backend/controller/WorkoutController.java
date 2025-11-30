package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

import com.spite.backend.model.Workout;
import com.spite.backend.model.Exercise;
import com.spite.backend.model.AssignedWorkout;
import com.spite.backend.dto.WorkoutWithExercises;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.AssignedWorkoutRepository;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutRepository workoutRepo;
    private final ExerciseRepository exerciseRepo;
    private final AssignedWorkoutRepository assignedRepo;

    public WorkoutController(
            WorkoutRepository workoutRepo,
            ExerciseRepository exerciseRepo,
            AssignedWorkoutRepository assignedRepo) {
        this.workoutRepo = workoutRepo;
        this.exerciseRepo = exerciseRepo;
        this.assignedRepo = assignedRepo;
    }

    @GetMapping
    public List<Workout> getAll(@RequestParam(required = false) String userId) {
        List<Workout> workouts;

        if (userId != null && !userId.isEmpty()) {
            workouts = workoutRepo.findByUserId(userId);
        } else {
            workouts = workoutRepo.findAll();
        }

        for (Workout w : workouts) {
            w.setExercises(resolveExercisesForWorkout(w));
        }

        return workouts;
    }

    @GetMapping("/{id}")
    public Workout getById(@PathVariable String id) {
        Workout w = workoutRepo.findById(id).orElse(null);
        if (w != null) {
            w.setExercises(resolveExercisesForWorkout(w));
        }
        return w;
    }

    @PostMapping
    public Workout add(@RequestBody Workout w) {
        return workoutRepo.save(w);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        workoutRepo.deleteById(id);
    }

    @GetMapping("/user/{userId}")
    public List<Workout> getByUser(@PathVariable String userId) {
        List<Workout> workouts = workoutRepo.findByUserId(userId);
        for (Workout w : workouts) {
            w.setExercises(resolveExercisesForWorkout(w));
        }
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

    @GetMapping("/client/{username}")
    public ResponseEntity<?> getClientWorkoutsWithExercises(@PathVariable String username) {
        var links = assignedRepo.findByClientUsername(username);
        var workoutIds = links.stream().map(AssignedWorkout::getWorkoutId).toList();
        var workouts = workoutRepo.findAllById(workoutIds);

        var result = workouts.stream().map(w -> {
            var exercises = resolveExercisesForWorkout(w);

            return new WorkoutWithExercises(
                    w.getId(),
                    w.getTitle(),
                    w.getSubtitle(),
                    w.getContent(),
                    exercises);
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkout(
            @PathVariable String id,
            @RequestBody Workout updated) {

        var opt = workoutRepo.findById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Workout not found");
        }

        Workout existing = opt.get();

        if (updated.getTitle() != null) {
            existing.setTitle(updated.getTitle());
        }

        if (updated.getSubtitle() != null) {
            existing.setSubtitle(updated.getSubtitle());
        }

        if (updated.getContent() != null) {
            existing.setContent(updated.getContent());
        }

        if (updated.getExerciseIds() != null) {
            existing.setExerciseIds(updated.getExerciseIds());
        }

        if (updated.getWorkoutExercises() != null) {
            existing.setWorkoutExercises(updated.getWorkoutExercises());
        }

        workoutRepo.save(existing);

        existing.setExercises(resolveExercisesForWorkout(existing));

        return ResponseEntity.ok(existing);
    }

    private List<Exercise> resolveExercisesForWorkout(Workout w) {
        if (w.getExerciseIds() != null && !w.getExerciseIds().isEmpty()) {
            return exerciseRepo.findAllById(w.getExerciseIds());
        }

        if (w.getWorkoutExercises() != null && !w.getWorkoutExercises().isEmpty()) {
            var ids = w.getWorkoutExercises().stream()
                    .map(we -> we.getExerciseId())
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            if (ids.isEmpty()) {
                return List.of();
            }

            return exerciseRepo.findAllById(ids);
        }

        return List.of();
    }

}
