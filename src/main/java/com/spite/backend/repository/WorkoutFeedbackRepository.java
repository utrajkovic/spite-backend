package com.spite.backend.repository;

import com.spite.backend.model.WorkoutFeedback;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkoutFeedbackRepository extends MongoRepository<WorkoutFeedback, String> {
    List<WorkoutFeedback> findByUserId(String userId);
}
