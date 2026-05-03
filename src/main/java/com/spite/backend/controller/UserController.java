package com.spite.backend.controller;

import com.spite.backend.dto.LoginResponse;
import com.spite.backend.model.FcmToken;
import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.model.UserSession;
import com.spite.backend.repository.FcmTokenRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.LoginRateLimitService;
import com.spite.backend.service.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthService sessionAuthService;
    private final LoginRateLimitService loginRateLimitService;
    private final InputValidationService validation;

    public UserController(UserRepository repo,
            FcmTokenRepository fcmTokenRepo,
            PasswordEncoder passwordEncoder,
            SessionAuthService sessionAuthService,
            LoginRateLimitService loginRateLimitService,
            InputValidationService validation) {
        this.repo = repo;
        this.fcmTokenRepo = fcmTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthService = sessionAuthService;
        this.loginRateLimitService = loginRateLimitService;
        this.validation = validation;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (validation.invalidUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (validation.invalidPassword(user.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid password format");
        }

        if (repo.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        user.setRole(Role.USER);

        String hashed = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashed);

        repo.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user, HttpServletRequest request) {
        if (validation.invalidUsername(user.getUsername()) || validation.invalidPassword(user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String rateLimitKey = user.getUsername() + "@" + ip;
        if (loginRateLimitService.isBlocked(rateLimitKey)) {
            return ResponseEntity.status(429).body("Too many login attempts. Please try again later.");
        }

        Optional<User> existing = repo.findByUsername(user.getUsername());

        if (existing.isEmpty()) {
            loginRateLimitService.registerFailure(rateLimitKey);
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        User dbUser = existing.get();

        if (dbUser.isBlocked()) {
            return ResponseEntity.status(403).body("Account blocked");
        }

        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            loginRateLimitService.registerFailure(rateLimitKey);
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        if (dbUser.getRole() == null) {
            dbUser.setRole(Role.USER);
            repo.save(dbUser);
        }

        loginRateLimitService.registerSuccess(rateLimitKey);
        UserSession session = sessionAuthService.createSession(dbUser);
        return ResponseEntity.ok(LoginResponse.from(dbUser, session));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sessionAuthService.invalidateByToken(authorization);
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/validate/{username}")
    public ResponseEntity<?> validateUser(@PathVariable String username) {
        Optional<User> user = repo.findByUsername(username);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("deleted");
        }
        if (user.get().isBlocked()) {
            return ResponseEntity.status(403).body("blocked");
        }
        return ResponseEntity.ok("active");
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
        if (validation.invalidUsername(username) || validation.isBlank(token) || validation.tooLong(token, 4096)) {
            return ResponseEntity.badRequest().body("Missing or invalid fields");
        }

        // Upsert - jedan token po korisniku
        FcmToken fcmToken = fcmTokenRepo.findByUsername(username)
                .orElse(new FcmToken(username, token));
        fcmToken.setToken(token);
        fcmTokenRepo.save(fcmToken);

        return ResponseEntity.ok("Token saved");
    }
}
