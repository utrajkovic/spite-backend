package com.spite.backend.controller;

import com.spite.backend.model.TrainerClientLink;
import com.spite.backend.model.TrainerInvite;
import com.spite.backend.repository.TrainerClientRepository;
import com.spite.backend.repository.TrainerInviteRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.PushNotificationService;

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
    private final TrainerInviteRepository inviteRepo;
    private final UserRepository userRepo;
    private final RoleGuardService guard;
    private final PushNotificationService pushService;

    public TrainerController(TrainerClientRepository linkRepo, TrainerInviteRepository inviteRepo,
            UserRepository userRepo, RoleGuardService guard, PushNotificationService pushService) {
        this.linkRepo = linkRepo;
        this.inviteRepo = inviteRepo;
        this.userRepo = userRepo;
        this.guard = guard;
        this.pushService = pushService;
    }

    // Trener šalje invite klijentu
    @PostMapping("/invite")
    public ResponseEntity<?> sendInvite(
            @RequestParam String trainerUsername,
            @RequestParam String clientUsername) {

        if (!guard.hasRole(trainerUsername, Role.TRAINER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only trainers can send invites.");
        }

        if (!userRepo.existsByUsername(clientUsername)) {
            return ResponseEntity.badRequest().body("Client not found");
        }

        if (linkRepo.existsByTrainerUsernameAndClientUsername(trainerUsername, clientUsername)) {
            return ResponseEntity.badRequest().body("Client already added");
        }

        if (inviteRepo.existsByTrainerUsernameAndClientUsernameAndStatus(trainerUsername, clientUsername, "PENDING")) {
            return ResponseEntity.badRequest().body("Invite already sent");
        }

        inviteRepo.save(new TrainerInvite(trainerUsername, clientUsername));
        pushService.sendToUser(clientUsername,
                "💪 New trainer invite",
                trainerUsername + " wants to be your trainer. Check your profile!",
                "invite", trainerUsername);
        return ResponseEntity.ok("Invite sent successfully");
    }

    // Klijent dohvata svoje pending invite-ove
    @GetMapping("/invites/{clientUsername}")
    public List<TrainerInvite> getPendingInvites(@PathVariable String clientUsername) {
        return inviteRepo.findByClientUsernameAndStatus(clientUsername, "PENDING");
    }

    // Klijent prihvata invite
    @PostMapping("/invite/{inviteId}/accept")
    public ResponseEntity<?> acceptInvite(@PathVariable String inviteId) {
        var opt = inviteRepo.findById(inviteId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Invite not found");

        TrainerInvite invite = opt.get();
        invite.setStatus("ACCEPTED");
        inviteRepo.save(invite);

        // Kreiraj link
        if (!linkRepo.existsByTrainerUsernameAndClientUsername(invite.getTrainerUsername(), invite.getClientUsername())) {
            linkRepo.save(new TrainerClientLink(invite.getTrainerUsername(), invite.getClientUsername()));
        }

        return ResponseEntity.ok("Invite accepted");
    }

    // Klijent odbija invite
    @PostMapping("/invite/{inviteId}/decline")
    public ResponseEntity<?> declineInvite(@PathVariable String inviteId) {
        var opt = inviteRepo.findById(inviteId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Invite not found");

        TrainerInvite invite = opt.get();
        invite.setStatus("DECLINED");
        inviteRepo.save(invite);

        return ResponseEntity.ok("Invite declined");
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
