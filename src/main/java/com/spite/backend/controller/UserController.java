package com.spite.backend.controller;

import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository repo;
    private PasswordEncoder passwordEncoder;

    public UserController(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
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

}
