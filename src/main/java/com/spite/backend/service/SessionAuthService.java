package com.spite.backend.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.model.UserSession;
import com.spite.backend.repository.UserSessionRepository;

@Service
public class SessionAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserSessionRepository sessionRepo;

    @Value("${auth.session.ttl-hours:24}")
    private long sessionTtlHours;

    public SessionAuthService(UserSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    public UserSession createSession(User user) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (sessionTtlHours * 60L * 60L * 1000L);

        UserSession session = new UserSession();
        session.setToken(generateToken());
        session.setUsername(user.getUsername());
        session.setRole(user.getRole());
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        return sessionRepo.save(session);
    }

    public Optional<UserSession> validateSession(String authorizationHeader) {
        purgeExpired();
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return Optional.empty();
        }

        Optional<UserSession> sessionOpt = sessionRepo.findByToken(token);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        UserSession session = sessionOpt.get();
        long now = System.currentTimeMillis();
        if (session.getExpiresAt() <= now) {
            sessionRepo.deleteByToken(session.getToken());
            return Optional.empty();
        }

        // Sliding renewal: svako korišćenje produžava rok, ali se u bazu upisuje
        // najviše ~jednom dnevno (da ne pravimo write na svaki zahtev).
        long ttlMs = sessionTtlHours * 60L * 60L * 1000L;
        long oneDayMs = 24L * 60L * 60L * 1000L;
        if (session.getExpiresAt() - now < ttlMs - oneDayMs) {
            session.setExpiresAt(now + ttlMs);
            sessionRepo.save(session);
        }

        return Optional.of(session);
    }

    public Optional<String> getUsername(String authorizationHeader) {
        return validateSession(authorizationHeader).map(UserSession::getUsername);
    }

    public boolean isSameUser(String authorizationHeader, String expectedUsername) {
        return validateSession(authorizationHeader)
                .map(s -> s.getUsername().equals(expectedUsername))
                .orElse(false);
    }

    public boolean hasAnyRole(String authorizationHeader, Role... roles) {
        EnumSet<Role> allowed = EnumSet.noneOf(Role.class);
        for (Role role : roles) {
            allowed.add(role);
        }
        return validateSession(authorizationHeader)
                .map(s -> allowed.contains(s.getRole()))
                .orElse(false);
    }

    public void invalidateByToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null) {
            sessionRepo.deleteByToken(token);
        }
    }

    public void invalidateByUsername(String username) {
        sessionRepo.deleteByUsername(username);
    }

    private void purgeExpired() {
        sessionRepo.deleteByExpiresAtLessThan(System.currentTimeMillis());
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix) || authorizationHeader.length() <= prefix.length()) {
            return null;
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }
}