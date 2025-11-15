package com.spite.backend.controller;

import com.spite.backend.model.TrainerClientLink;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.spite.backend.service.RoleGuardService;
import com.spite.backend.model.Role;

@RestController
@RequestMapping("/api/trainer")
@CrossOrigin(origins = "*")
public class TrainerController {

    private final TrainerClientRepository linkRepo;
    private final UserRepository userRepo;
    private RoleGuardService guard;

    public TrainerController(TrainerClientRepository linkRepo, UserRepository userRepo, RoleGuardService guard) {
        this.linkRepo = linkRepo;
        this.userRepo = userRepo;
        this.guard = guard;
    }

    @PostMapping("/add-client")
    public ResponseEntity<?> addClient(
            @RequestParam String trainerUsername,
            @RequestParam String clientUsername) {

        if (!guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only trainers can add clients.");
        }

        if (!userRepo.existsByUsername(trainerUsername) || !userRepo.existsByUsername(clientUsername)) {
            return ResponseEntity.badRequest().body("Trainer or client not found");
        }

        if (linkRepo.existsByTrainerUsernameAndClientUsername(trainerUsername, clientUsername)) {
            return ResponseEntity.badRequest().body("Client already added");
        }

        TrainerClientLink link = new TrainerClientLink(trainerUsername, clientUsername);
        linkRepo.save(link);
        return ResponseEntity.ok("Client added successfully");
    }

    @GetMapping("/clients/{trainerUsername}")
    public List<TrainerClientLink> getClients(@PathVariable String trainerUsername) {
        return linkRepo.findByTrainerUsername(trainerUsername);
    }

    @DeleteMapping("/remove-client")
    public ResponseEntity<String> removeClient(
            @RequestParam String trainerUsername,
            @RequestParam String clientUsername) {

        List<TrainerClientLink> links = linkRepo.findByTrainerUsername(trainerUsername);
        links.stream()
                .filter(l -> l.getClientUsername().equals(clientUsername))
                .forEach(l -> linkRepo.deleteById(l.getId()));

        return ResponseEntity.ok("Client removed successfully");
    }
}
