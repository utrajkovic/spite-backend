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
    private final RoleGuardService guard;

    public UserAdminController(UserRepository repo, RoleGuardService guard) {
        this.repo = repo;
        this.guard = guard;
    }

    @GetMapping
    public List<User> listAll() {
        return repo.findAll();
    }

    @PutMapping("/{username}/role")
    public ResponseEntity<String> updateRole(
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam Role role) {

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

    @PutMapping("/{username}/password")
    public ResponseEntity<String> updatePassword(
            @RequestParam String adminUsername,
            @PathVariable String username,
            @RequestParam String newPassword) {

        if (!guard.hasRole(adminUsername, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can change passwords.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = optUser.get();
        user.setPassword(newPassword); // TODO: hash password
        repo.save(user);

        return ResponseEntity.ok("Password updated successfully.");
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteUser(
            @RequestParam String adminUsername,
            @PathVariable String username) {

        if (!guard.hasRole(adminUsername, Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: only admins can delete users.");
        }

        Optional<User> optUser = repo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        repo.delete(optUser.get());
        return ResponseEntity.ok("User deleted.");
    }

}
