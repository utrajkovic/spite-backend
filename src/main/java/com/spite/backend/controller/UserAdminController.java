package com.spite.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.RoleGuardService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class UserAdminController {

    private final UserRepository repo;
    private RoleGuardService guard;

    public UserAdminController(UserRepository repo, RoleGuardService guard) {
        this.repo = repo;
        this.guard = guard;
    }

    @GetMapping
    public List<User> listAll() {
        return repo.findAll();
    }

    @PutMapping("/users/{username}/role")
    public ResponseEntity<String> updateRole(
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam Role role) {

        // 🔹 Samo admin može da menja role
        if (!guard.hasRole(adminUsername, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can change roles.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = optUser.get();
        user.setRole(role);
        repo.save(user);

        return ResponseEntity.ok("Role updated to " + role);
    }

}
