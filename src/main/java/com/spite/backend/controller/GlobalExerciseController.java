package com.spite.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.GlobalExercise;
import com.spite.backend.model.Role;
import com.spite.backend.repository.GlobalExerciseRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@RequestMapping("/api/global-exercises")
@CrossOrigin(origins = "*")
public class GlobalExerciseController {

    private final GlobalExerciseRepository repo;
    private final SessionAuthService sessionAuthService;
    private final RoleGuardService guard;
    private final InputValidationService validation;

    public GlobalExerciseController(GlobalExerciseRepository repo,
                                     SessionAuthService sessionAuthService,
                                     RoleGuardService guard,
                                     InputValidationService validation) {
        this.repo = repo;
        this.sessionAuthService = sessionAuthService;
        this.guard = guard;
        this.validation = validation;
    }

    // Svi ulogovani korisnici mogu da vide globalne vežbe
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String username = sessionAuthService.getUsername(authorization).orElse(null);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        return ResponseEntity.ok(repo.findAllByOrderBySortOrderAsc());
    }

    // Filtriraj po mišićnim grupama (za generator)
    @GetMapping("/by-muscles")
    public ResponseEntity<?> getByMuscles(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam List<String> muscles) {
        String username = sessionAuthService.getUsername(authorization).orElse(null);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        List<GlobalExercise> exercises = repo.findByMuscleGroupsIn(muscles);
        exercises.sort((a, b) -> {
            int catCompare = categoryOrder(a.getCategory()) - categoryOrder(b.getCategory());
            if (catCompare != 0) return catCompare;
            return Integer.compare(a.getSortOrder(), b.getSortOrder());
        });
        return ResponseEntity.ok(exercises);
    }

    private int categoryOrder(String category) {
        if ("compound".equalsIgnoreCase(category)) return 0;
        if ("isolation".equalsIgnoreCase(category)) return 1;
        return 2;
    }

    // ─── ADMIN ONLY ───

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody GlobalExercise exercise) {
        String username = sessionAuthService.getUsername(authorization).orElse(null);
        if (username == null || !guard.hasRole(username, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }
        if (exercise.getName() == null || exercise.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Exercise name is required");
        }
        if (exercise.getMuscleGroups() == null || exercise.getMuscleGroups().isEmpty()) {
            return ResponseEntity.badRequest().body("At least one muscle group is required");
        }
        exercise.setId(null);
        return ResponseEntity.ok(repo.save(exercise));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody GlobalExercise exercise) {
        String username = sessionAuthService.getUsername(authorization).orElse(null);
        if (username == null || !guard.hasRole(username, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }
        var existing = repo.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.badRequest().body("Exercise not found");
        }
        exercise.setId(id);
        return ResponseEntity.ok(repo.save(exercise));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        String username = sessionAuthService.getUsername(authorization).orElse(null);
        if (username == null || !guard.hasRole(username, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }
        if (!repo.existsById(id)) {
            return ResponseEntity.badRequest().body("Exercise not found");
        }
        repo.deleteById(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}