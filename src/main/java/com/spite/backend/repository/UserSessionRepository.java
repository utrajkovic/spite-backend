package com.spite.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.UserSession;

public interface UserSessionRepository extends MongoRepository<UserSession, String> {
    Optional<UserSession> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUsername(String username);
    void deleteByExpiresAtLessThan(long timestamp);
}