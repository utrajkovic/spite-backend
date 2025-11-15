package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

import com.spite.backend.model.Workout;
import com.spite.backend.model.ClientWorkoutLink;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.repository.ClientWorkoutLinkRepository;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/trainer")
@CrossOrigin(origins = "*")
public class TrainerWorkoutController {

    private final WorkoutRepository workoutRepo;
    private final ClientWorkoutLinkRepository linkRepo;
    private final UserRepository userRepo;
    private final ExerciseRepository exerciseRepo;

    public TrainerWorkoutController(
            WorkoutRepository workoutRepo,
            ClientWorkoutLinkRepository linkRepo,
            UserRepository userRepo,
            ExerciseRepository exerciseRepo) {
        this.workoutRepo = workoutRepo;
        this.linkRepo = linkRepo;
        this.userRepo = userRepo;
        this.exerciseRepo = exerciseRepo;
    }

    @GetMapping("/workouts/{trainerUsername}")
    public ResponseEntity<?> getTrainerWorkouts(@PathVariable String trainerUsername) {
        var trainerOpt = userRepo.findByUsername(trainerUsername);
        if (trainerOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Trainer not found");
        }

        String trainerId = trainerOpt.get().getId();
        List<Workout> workouts = workoutRepo.findByUserId(trainerId);
        return ResponseEntity.ok(workouts);
    }

    @GetMapping("/client-workouts/{clientUsername}")
    public ResponseEntity<?> getClientWorkouts(@PathVariable String clientUsername) {
        List<ClientWorkoutLink> links = linkRepo.findByClientUsername(clientUsername);
        List<String> workoutIds = links.stream().map(ClientWorkoutLink::getWorkoutId).toList();
        List<Workout> workouts = workoutRepo.findAllById(workoutIds);
        return ResponseEntity.ok(workouts);
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignWorkout(
            @RequestParam String trainer,
            @RequestParam String client,
            @RequestParam String workoutId) {
        Optional<ClientWorkoutLink> existing = linkRepo.findByClientUsernameAndWorkoutId(client, workoutId);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Workout already assigned to this client");
        }

        ClientWorkoutLink link = new ClientWorkoutLink();
        link.setTrainerUsername(trainer);
        link.setClientUsername(client);
        link.setWorkoutId(workoutId);

        linkRepo.save(link);
        return ResponseEntity.ok("Workout assigned successfully");
    }

    @DeleteMapping("/unassign")
    public ResponseEntity<?> unassignWorkout(
            @RequestParam String client,
            @RequestParam String workoutId) {
        Optional<ClientWorkoutLink> existing = linkRepo.findByClientUsernameAndWorkoutId(client, workoutId);
        if (existing.isEmpty()) {
            return ResponseEntity.badRequest().body("Assignment not found");
        }

        linkRepo.delete(existing.get());
        return ResponseEntity.ok("Workout unassigned successfully");
    }

    @GetMapping("/client-workouts-full/{clientUsername}")
    public ResponseEntity<?> getClientWorkoutsFull(@PathVariable String clientUsername) {
        List<ClientWorkoutLink> links = linkRepo.findByClientUsername(clientUsername);
        if (links.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<String> workoutIds = links.stream()
                .map(ClientWorkoutLink::getWorkoutId)
                .toList();

        List<Workout> workouts = workoutRepo.findAllById(workoutIds);

        // Uključi vežbe
        for (Workout w : workouts) {
            if (w.getExerciseIds() != null && !w.getExerciseIds().isEmpty()) {
                List<com.spite.backend.model.Exercise> exList = new ArrayList<>();
                for (String exId : w.getExerciseIds()) {
                    var exOpt = exerciseRepo.findById(exId);
                    exOpt.ifPresent(exList::add);
                }
                w.setExercises(exList);
            } else {
                w.setExercises(List.of());
            }
        }

        return ResponseEntity.ok(workouts);
    }

}
