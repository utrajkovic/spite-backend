package com.spite.backend.controller;

import com.spite.backend.model.*;
import com.spite.backend.repository.*;
import com.spite.backend.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@CrossOrigin(origins = "*")
public class ShareController {

    private final ShareInviteRepository inviteRepo;
    private final ExerciseRepository exerciseRepo;
    private final WorkoutRepository workoutRepo;
    private final UserRepository userRepo;
    private final PushNotificationService pushService;

    public ShareController(ShareInviteRepository inviteRepo,
                           ExerciseRepository exerciseRepo,
                           WorkoutRepository workoutRepo,
                           UserRepository userRepo,
                           PushNotificationService pushService) {
        this.inviteRepo = inviteRepo;
        this.exerciseRepo = exerciseRepo;
        this.workoutRepo = workoutRepo;
        this.userRepo = userRepo;
        this.pushService = pushService;
    }

    // Pošalji share invite
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body) {
        String fromUsername = (String) body.get("fromUsername");
        String toUsername = (String) body.get("toUsername");
        String type = (String) body.get("type");
        List<String> itemIds = (List<String>) body.get("itemIds");

        if (!userRepo.existsByUsername(toUsername)) {
            return ResponseEntity.badRequest().body("User not found: " + toUsername);
        }

        if (fromUsername.equals(toUsername)) {
            return ResponseEntity.badRequest().body("Cannot share with yourself");
        }

        // Dohvati nazive za snapshot
        List<String> itemNames;
        if ("exercise".equals(type)) {
            itemNames = exerciseRepo.findAllById(itemIds).stream()
                    .map(Exercise::getName).toList();
        } else {
            itemNames = workoutRepo.findAllById(itemIds).stream()
                    .map(Workout::getTitle).toList();
        }

        ShareInvite invite = new ShareInvite(fromUsername, toUsername, type, itemIds, itemNames);
        inviteRepo.save(invite);

        // Push notifikacija
        pushService.sendToUser(toUsername,
                "📦 " + fromUsername + " shared " + type + "s with you",
                String.join(", ", itemNames),
                "share_invite", fromUsername);

        return ResponseEntity.ok("Invite sent");
    }

    // Dohvati pending invites za korisnika
    @GetMapping("/pending/{username}")
    public List<ShareInvite> getPending(@PathVariable String username) {
        return inviteRepo.findByToUsernameAndStatus(username, "PENDING");
    }

    // Prihvati - kopiraj items u kolekciju primaoca
    @PostMapping("/{inviteId}/accept")
    public ResponseEntity<?> accept(@PathVariable String inviteId) {
        var opt = inviteRepo.findById(inviteId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Invite not found");

        ShareInvite invite = opt.get();
        invite.setStatus("ACCEPTED");
        inviteRepo.save(invite);

        // Dohvati primaoca
        var toUserOpt = userRepo.findByUsername(invite.getToUsername());
        if (toUserOpt.isEmpty()) return ResponseEntity.badRequest().body("User not found");
        String toUserId = toUserOpt.get().getId();

        if ("exercise".equals(invite.getType())) {
            // Kopiraj vežbe - novi dokumenti sa userId primaoca
            exerciseRepo.findAllById(invite.getItemIds()).forEach(ex -> {
                Exercise copy = new Exercise();
                copy.setName(ex.getName());
                copy.setDescription(ex.getDescription());
                copy.setVideoUrl(ex.getVideoUrl());
                copy.setUserId(toUserId);
                exerciseRepo.save(copy);
            });
        } else {
            // Kopiraj workouts
            workoutRepo.findAllById(invite.getItemIds()).forEach(w -> {
                Workout copy = new Workout();
                copy.setTitle(w.getTitle());
                copy.setSubtitle(w.getSubtitle());
                copy.setContent(w.getContent());
                copy.setItems(w.getItems());
                copy.setExerciseIds(w.getExerciseIds());
                copy.setUserId(toUserId);
                workoutRepo.save(copy);
            });
        }

        return ResponseEntity.ok("Accepted");
    }

    // Odbij
    @PostMapping("/{inviteId}/decline")
    public ResponseEntity<?> decline(@PathVariable String inviteId) {
        var opt = inviteRepo.findById(inviteId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Invite not found");
        ShareInvite invite = opt.get();
        invite.setStatus("DECLINED");
        inviteRepo.save(invite);
        return ResponseEntity.ok("Declined");
    }
}
