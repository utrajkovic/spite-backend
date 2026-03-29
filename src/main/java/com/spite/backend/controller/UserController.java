package com.spite.backend.controller;

import com.spite.backend.model.FcmToken;
import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.repository.FcmTokenRepository;
import com.spite.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository repo;
    private final FcmTokenRepository fcmTokenRepo;
    private PasswordEncoder passwordEncoder;

    public UserController(UserRepository repo, FcmTokenRepository fcmTokenRepo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.fcmTokenRepo = fcmTokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (repo.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        user.setRole(Role.USER);

        String hashed = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashed);

        User saved = repo.save(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> existing = repo.findByUsername(user.getUsername());

        if (existing.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        User dbUser = existing.get();

        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        if (dbUser.getRole() == null) {
            dbUser.setRole(Role.USER);
            repo.save(dbUser);
        }

        return ResponseEntity.ok(dbUser);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Optional<User> user = repo.findByUsername(username);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }

    @GetMapping("/exists/{username}")
    public ResponseEntity<?> checkUserExists(@PathVariable String username) {
        boolean exists = repo.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<?> saveFcmToken(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String token = body.get("token");
        if (username == null || token == null) return ResponseEntity.badRequest().body("Missing fields");

        // Upsert - jedan token po korisniku
        FcmToken fcmToken = fcmTokenRepo.findByUsername(username)
                .orElse(new FcmToken(username, token));
        fcmToken.setToken(token);
        fcmTokenRepo.save(fcmToken);

        return ResponseEntity.ok("Token saved");
    }
}
