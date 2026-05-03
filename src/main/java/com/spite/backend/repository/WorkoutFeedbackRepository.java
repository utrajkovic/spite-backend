package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.WorkoutFeedback;

public interface WorkoutFeedbackRepository extends MongoRepository<WorkoutFeedback, String> {
    List<WorkoutFeedback> findByUserId(String userId);
    void deleteByUserId(String userId);
}
