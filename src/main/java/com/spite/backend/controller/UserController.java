package com.spite.backend.controller;

import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (repo.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setRole(Role.USER);
        User saved = repo.save(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> existing = repo.findByUsername(user.getUsername());

        if (existing.isPresent() && existing.get().getPassword().equals(user.getPassword())) {
            User logged = existing.get();

            if (logged.getRole() == null) {
                logged.setRole(Role.USER);
                repo.save(logged);
            }

            return ResponseEntity.ok(logged);
        }

        return ResponseEntity.status(401).body("Invalid username or password");
    }

}
