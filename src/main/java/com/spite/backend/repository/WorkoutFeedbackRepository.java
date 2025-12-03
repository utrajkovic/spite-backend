package com.spite.backend.repository;

import com.spite.backend.model.WorkoutFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkoutFeedbackRepository
        extends MongoRepository<WorkoutFeedback, String> {
}
