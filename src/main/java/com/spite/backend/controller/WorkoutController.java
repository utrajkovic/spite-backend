package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.spite.backend.model.Workout;
import com.spite.backend.model.Exercise;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.repository.ExerciseRepository;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutRepository workoutRepo;
    private final ExerciseRepository exerciseRepo;

    public WorkoutController(WorkoutRepository workoutRepo, ExerciseRepository exerciseRepo) {
        this.workoutRepo = workoutRepo;
        this.exerciseRepo = exerciseRepo;
    }

    @GetMapping
    public List<Workout> getAll() {
        List<Workout> workouts = workoutRepo.findAll();

        // 🔹 Učitaj sve vežbe za svaki trening
        for (Workout w : workouts) {
            List<Exercise> exList = exerciseRepo.findAllById(w.getExerciseIds());
            w.setExercises(exList);
        }

        return workouts;
    }

    @GetMapping("/{id}")
    public Workout getById(@PathVariable String id) {
        Workout w = workoutRepo.findById(id).orElse(null);
        if (w != null) {
            List<Exercise> exList = exerciseRepo.findAllById(w.getExerciseIds());
            w.setExercises(exList);
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
}
