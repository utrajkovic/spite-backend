package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.WorkoutRepository;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutRepository repo;

    public WorkoutController(WorkoutRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Workout> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Workout getById(@PathVariable String id) {
        return repo.findById(id).orElse(null);
    }

    @PostMapping
    public Workout add(@RequestBody Workout w) {
        return repo.save(w);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repo.deleteById(id);
    }
}
