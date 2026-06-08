package com.spite.backend.controller;

import com.spite.backend.dto.LoginResponse;
import com.spite.backend.model.EmailVerificationToken;
import com.spite.backend.model.FcmToken;
import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.model.UserSession;
import com.spite.backend.model.WorkoutReminder;
import com.spite.backend.repository.EmailVerificationTokenRepository;
import com.spite.backend.repository.FcmTokenRepository;
import com.spite.backend.repository.UserRepository;
import com.spite.backend.service.CloudinaryService;
import com.spite.backend.service.EmailService;
import com.spite.backend.service.InputValidationService;
import com.spite.backend.service.LoginRateLimitService;
import com.spite.backend.service.SessionAuthService;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private final EmailService emailService;
    private final EmailVerificationTokenRepository tokenRepo;
    private final CloudinaryService cloudinaryService;

    @Value("${app.public-url:https://spite-backend.fly.dev}")
    private String publicUrl;

    public UserController(UserRepository repo,
            FcmTokenRepository fcmTokenRepo,
            PasswordEncoder passwordEncoder,
            SessionAuthService sessionAuthService,
            LoginRateLimitService loginRateLimitService,
            InputValidationService validation,
            EmailService emailService,
            EmailVerificationTokenRepository tokenRepo,
            CloudinaryService cloudinaryService) {
        this.repo = repo;
        this.fcmTokenRepo = fcmTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthService = sessionAuthService;
        this.loginRateLimitService = loginRateLimitService;
        this.validation = validation;
        this.emailService = emailService;
        this.tokenRepo = tokenRepo;
        this.cloudinaryService = cloudinaryService;
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

        String email = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
        if (email != null && !email.isEmpty() && !EmailService.isValidEmail(email)) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        user.setRole(Role.USER);
        user.setEmail(email == null || email.isEmpty() ? null : email);
        user.setEmailVerified(false);

        String hashed = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashed);

        repo.save(user);

        if (user.getEmail() != null) {
            sendVerificationEmail(user);
        }
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

    // Dodaj/promeni email za ulogovanog korisnika -> šalje verifikaciju
    @PutMapping("/email")
    public ResponseEntity<?> setEmail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username,
            @RequestParam String email) {

        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!EmailService.isValidEmail(normalized)) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        Optional<User> opt = repo.findByUsername(username);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = opt.get();
        user.setEmail(normalized);
        user.setEmailVerified(false);
        repo.save(user);

        sendVerificationEmail(user);
        return ResponseEntity.ok("Verification email sent");
    }

    // Ponovo pošalji verifikacioni email
    @PostMapping("/email/resend")
    public ResponseEntity<?> resendVerification(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username) {

        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Optional<User> opt = repo.findByUsername(username);
        if (opt.isEmpty() || opt.get().getEmail() == null) {
            return ResponseEntity.badRequest().body("No email to verify");
        }
        if (opt.get().isEmailVerified()) {
            return ResponseEntity.ok("Email already verified");
        }

        sendVerificationEmail(opt.get());
        return ResponseEntity.ok("Verification email sent");
    }

    // Podešavanje dnevnog podsetnika (toggle + vreme)
    @PutMapping("/reminder")
    public ResponseEntity<?> setReminder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username,
            @RequestParam boolean enabled,
            @RequestParam(required = false) String time) {

        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Optional<User> opt = repo.findByUsername(username);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = opt.get();
        user.setDailyReminderEnabled(enabled);
        if (time != null && time.matches("^\\d{2}:\\d{2}$")) {
            user.setDailyReminderTime(time);
        }
        repo.save(user);
        return ResponseEntity.ok("Reminder settings saved");
    }

    // Telo zahteva za korisnički (CLIENT) podsetnik
    public static class ClientReminderRequest {
        public boolean enabled;
        public String mode;                       // SESSIONS | CUSTOM
        public String time;                       // HH:mm (SESSIONS režim)
        public List<WorkoutReminder> customReminders;
    }

    // Podešavanje korisničkog podsetnika za trening (SESSIONS ili CUSTOM)
    @PutMapping("/client-reminder")
    public ResponseEntity<?> setClientReminder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username,
            @RequestBody ClientReminderRequest body) {

        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Optional<User> opt = repo.findByUsername(username);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = opt.get();
        user.setClientReminderEnabled(body.enabled);

        if ("CUSTOM".equals(body.mode) || "SESSIONS".equals(body.mode)) {
            user.setClientReminderMode(body.mode);
        }
        if (body.time != null && body.time.matches("^\\d{2}:\\d{2}$")) {
            user.setClientReminderTime(body.time);
        }

        // Validiraj i normalizuj custom stavke (preskoči nevalidne)
        List<WorkoutReminder> clean = new java.util.ArrayList<>();
        if (body.customReminders != null) {
            for (WorkoutReminder r : body.customReminders) {
                if (r == null || r.getTime() == null || !r.getTime().matches("^\\d{2}:\\d{2}$")) continue;
                if (r.getDays() == null || r.getDays().isEmpty()) continue;
                List<Integer> days = new java.util.ArrayList<>();
                for (Integer d : r.getDays()) {
                    if (d != null && d >= 1 && d <= 7 && !days.contains(d)) days.add(d);
                }
                if (days.isEmpty()) continue;
                r.setDays(days);
                if (r.getNote() == null) r.setNote("");
                clean.add(r);
            }
        }
        user.setCustomReminders(clean);

        repo.save(user);
        return ResponseEntity.ok("Client reminder settings saved");
    }

    // Upload profilne slike (slika je već isečena na klijentu)
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username,
            @RequestParam("image") MultipartFile image) {

        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("No image");
        }

        Optional<User> opt = repo.findByUsername(username);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        try {
            String url = cloudinaryService.upload(image, ObjectUtils.asMap(
                    "folder", "spite/avatars",
                    "resource_type", "image",
                    "transformation", new Transformation()
                            .width(400).height(400).crop("fill").gravity("center")
                            .quality("auto").fetchFormat("auto")));

            User user = opt.get();
            user.setAvatarUrl(url);
            repo.save(user);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed");
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username) {

        if (validation.invalidUsername(username)) {
            return ResponseEntity.badRequest().body("Invalid username format");
        }
        if (!sessionAuthService.isSameUser(authorization, username)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Optional<User> opt = repo.findByUsername(username);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        User user = opt.get();
        user.setAvatarUrl(null);
        repo.save(user);
        return ResponseEntity.ok("Avatar removed");
    }

    // Bulk dohvat avatara po korisničkim imenima (za liste/chat)
    @PostMapping("/avatars")
    public ResponseEntity<?> getAvatars(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody List<String> usernames) {

        if (sessionAuthService.getUsername(authorization).isEmpty()) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Map<String, String> result = new HashMap<>();
        if (usernames == null || usernames.isEmpty()) {
            return ResponseEntity.ok(result);
        }
        List<String> capped = usernames.size() > 200 ? usernames.subList(0, 200) : usernames;
        for (User u : repo.findByUsernameIn(capped)) {
            result.put(u.getUsername(), u.getAvatarUrl());
        }
        return ResponseEntity.ok(result);
    }

    // Verifikacioni link iz mejla (otvara se u browseru) -> vraća malu HTML stranicu
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        Optional<EmailVerificationToken> opt = tokenRepo.findByToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(verifyPage("Link nije važeći", "Verifikacioni link je nevažeći ili je već iskorišćen.", false));
        }
        EmailVerificationToken t = opt.get();
        if (t.getExpiresAt() <= System.currentTimeMillis()) {
            tokenRepo.delete(t);
            return ResponseEntity.ok(verifyPage("Link je istekao", "Zatraži novi verifikacioni email iz aplikacije.", false));
        }

        Optional<User> userOpt = repo.findByUsername(t.getUsername());
        if (userOpt.isEmpty() || userOpt.get().getEmail() == null
                || !userOpt.get().getEmail().equalsIgnoreCase(t.getEmail())) {
            tokenRepo.delete(t);
            return ResponseEntity.ok(verifyPage("Link nije važeći", "Email adresa je u međuvremenu promenjena.", false));
        }

        User user = userOpt.get();
        user.setEmailVerified(true);
        repo.save(user);
        tokenRepo.deleteByUsername(user.getUsername());

        return ResponseEntity.ok(verifyPage("Email verifikovan ✓", "Tvoj email je uspešno potvrđen. Možeš da zatvoriš ovu stranicu.", true));
    }

    private void sendVerificationEmail(User user) {
        if (user.getEmail() == null) return;

        tokenRepo.deleteByUsername(user.getUsername());

        String token = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + (24L * 60L * 60L * 1000L);
        tokenRepo.save(new EmailVerificationToken(user.getUsername(), token, user.getEmail(), expiresAt));

        String link = publicUrl + "/api/users/verify-email?token=" + token;
        String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:auto\">"
                + "<h2 style=\"color:#111\">Verify your email</h2>"
                + "<p>Hi <strong>" + user.getUsername() + "</strong>,</p>"
                + "<p>Confirm your email so your trainer can notify you about scheduled sessions.</p>"
                + "<p style=\"margin:24px 0\"><a href=\"" + link + "\" "
                + "style=\"background:#111;color:#fff;padding:12px 22px;border-radius:10px;text-decoration:none;font-weight:700\">Verify email</a></p>"
                + "<p style=\"color:#888;font-size:12px\">Link expires in 24h. If you didn't request this, ignore it.</p>"
                + "<p style=\"color:#888;font-size:12px\">Spite</p></div>";

        emailService.send(user.getEmail(), "Verify your email - Spite", html);
    }

    private String verifyPage(String title, String message, boolean ok) {
        String color = ok ? "#16a34a" : "#dc2626";
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + title + "</title></head>"
                + "<body style=\"font-family:sans-serif;background:#0a0a0a;color:#f2f2f2;display:flex;"
                + "align-items:center;justify-content:center;min-height:100vh;margin:0\">"
                + "<div style=\"text-align:center;max-width:420px;padding:32px\">"
                + "<h1 style=\"color:" + color + ";font-size:24px\">" + title + "</h1>"
                + "<p style=\"color:#9a9a9a;font-size:15px;line-height:1.6\">" + message + "</p>"
                + "</div></body></html>";
    }
}
