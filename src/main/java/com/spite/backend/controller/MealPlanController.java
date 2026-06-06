package com.spite.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spite.backend.model.MealPlan;
import com.spite.backend.model.Role;
import com.spite.backend.repository.MealPlanRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.PushNotificationService;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.service.SessionAuthService;

@RestController
@RequestMapping("/api/meals")
@CrossOrigin(origins = "*")
public class MealPlanController {

    private final MealPlanRepository mealPlanRepo;
    private final SessionAuthService sessionAuthService;
    private final RoleGuardService guard;
    private final InputValidationService validation;
    private final PushNotificationService pushService;

    public MealPlanController(
            MealPlanRepository mealPlanRepo,
            SessionAuthService sessionAuthService,
            RoleGuardService guard,
            InputValidationService validation,
            PushNotificationService pushService) {
        this.mealPlanRepo = mealPlanRepo;
        this.sessionAuthService = sessionAuthService;
        this.guard = guard;
        this.validation = validation;
        this.pushService = pushService;
    }

    // Klijent vidi svoj plan; trener vidi plan bilo kog klijenta
    @GetMapping("/client/{clientUsername}")
    public ResponseEntity<?> getMealPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String clientUsername) {

        if (validation.invalidUsername(clientUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }

        String actor = sessionAuthService.getUsername(authorization).orElse(null);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
        boolean isOwner = actor.equals(clientUsername);
        boolean isTrainer = guard.hasRole(actor, Role.TRAINER);
        if (!isOwner && !isTrainer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        return ResponseEntity.ok(mealPlanRepo.findByClientUsername(clientUsername).orElse(null));
    }

    // Trener kreira/menja plan ishrane za klijenta (jedan aktivan plan po klijentu)
    @PutMapping
    public ResponseEntity<?> saveMealPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String trainerUsername,
            @RequestParam String clientUsername,
            @RequestBody List<MealPlan.Meal> meals) {

        if (validation.invalidUsername(trainerUsername) || validation.invalidUsername(clientUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername)
                || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        MealPlan plan = mealPlanRepo.findByClientUsername(clientUsername).orElseGet(MealPlan::new);
        plan.setClientUsername(clientUsername);
        plan.setTrainerUsername(trainerUsername);
        plan.setMeals(meals == null ? List.of() : meals);
        plan.setUpdatedAt(System.currentTimeMillis());
        mealPlanRepo.save(plan);

        pushService.sendToUser(clientUsername,
                "🥗 New meal plan",
                trainerUsername + " updated your meal plan. Check your profile!",
                "meal", trainerUsername);

        return ResponseEntity.ok("Meal plan saved");
    }

    // Trener briše plan ishrane klijenta
    @DeleteMapping("/client/{clientUsername}")
    public ResponseEntity<?> deleteMealPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String trainerUsername,
            @PathVariable String clientUsername) {

        if (validation.invalidUsername(trainerUsername) || validation.invalidUsername(clientUsername)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, trainerUsername)
                || !guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }

        mealPlanRepo.deleteByClientUsername(clientUsername);
        return ResponseEntity.ok("Meal plan deleted");
    }
}
