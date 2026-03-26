package com.spite.backend.repository;

import com.spite.backend.model.WorkoutFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WorkoutFeedbackRepository extends MongoRepository<WorkoutFeedback, String> {
    List<WorkoutFeedback> findByUserId(String userId);
}
