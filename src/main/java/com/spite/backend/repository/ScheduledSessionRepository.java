package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.ScheduledSession;

public interface ScheduledSessionRepository extends MongoRepository<ScheduledSession, String> {
    List<ScheduledSession> findByClientUsername(String clientUsername);
    List<ScheduledSession> findByTrainerUsername(String trainerUsername);
    List<ScheduledSession> findByStartTimeBetween(long start, long end);
    void deleteByClientUsername(String clientUsername);
    void deleteByTrainerUsername(String trainerUsername);
}
