package com.spite.backend.controller;

import com.spite.backend.model.AssignedWorkout;
import com.spite.backend.model.CompletedWorkout;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.AssignedWorkoutRepository;
import com.spite.backend.repository.CompletedWorkoutRepository;
import com.spite.backend.repository.DailyCheckInRepository;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.WorkoutRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.SessionAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agenda")
@CrossOrigin(origins = "*")
public class AgendaController {

    private final SessionAuthService sessionAuthService;
    private final InputValidationService validation;
    private final AssignedWorkoutRepository assignedRepo;
    private final WorkoutRepository workoutRepo;
    private final CompletedWorkoutRepository completedRepo;
    private final DailyCheckInRepository checkInRepo;
    private final TrainerClientRepository trainerClientRepo;

    public AgendaController(
            SessionAuthService sessionAuthService,
            InputValidationService validation,
            AssignedWorkoutRepository assignedRepo,
            WorkoutRepository workoutRepo,
            CompletedWorkoutRepository completedRepo,
            DailyCheckInRepository checkInRepo,
            TrainerClientRepository trainerClientRepo) {
        this.sessionAuthService = sessionAuthService;
        this.validation = validation;
        this.assignedRepo = assignedRepo;
        this.workoutRepo = workoutRepo;
        this.completedRepo = completedRepo;
        this.checkInRepo = checkInRepo;
        this.trainerClientRepo = trainerClientRepo;
    }

    @GetMapping("/today/{username}")
    public ResponseEntity<?> getTodayAgenda(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        String dateKey = LocalDate.now(ZoneId.systemDefault()).toString();
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        long startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<AssignedWorkout> assigned = assignedRepo.findByClientUsername(username);
        List<CompletedWorkout> completed = completedRepo.findByUsername(username);

        long completedToday = completed.stream()
                .filter(c -> c.getCompletedAt() >= startOfDay && c.getCompletedAt() <= now)
                .count();

        boolean checkInSubmitted = checkInRepo.existsByUsernameAndDateKey(username, dateKey);

        String trainerUsername = trainerClientRepo.findByClientUsername(username)
                .stream()
                .findFirst()
                .map(link -> link.getTrainerUsername())
                .orElse(null);

        List<Map<String, Object>> assignedWorkouts = assigned.stream().map(link -> {
            Map<String, Object> row = new HashMap<>();
            row.put("workoutId", link.getWorkoutId());
            row.put("note", link.getNote());
            Workout workout = workoutRepo.findById(link.getWorkoutId()).orElse(null);
            row.put("title", workout != null ? workout.getTitle() : "Workout");
            return row;
        }).toList();

        List<Map<String, Object>> tasks = List.of(
                Map.of("key", "checkin", "title", "Submit daily check-in", "done", checkInSubmitted),
                Map.of("key", "workout", "title", "Complete at least one workout", "done", completedToday > 0)
        );

        Map<String, Object> response = new HashMap<>();
        response.put("dateKey", dateKey);
        response.put("trainerUsername", trainerUsername);
        response.put("assignedCount", assigned.size());
        response.put("completedToday", completedToday);
        response.put("checkInSubmitted", checkInSubmitted);
        response.put("tasks", tasks);
        response.put("assignedWorkouts", assignedWorkouts);

        return ResponseEntity.ok(response);
    }
}
