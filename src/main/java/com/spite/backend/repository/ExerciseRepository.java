package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.spite.backend.model.Exercise;

public interface ExerciseRepository extends MongoRepository<Exercise, String> {
    List<Exercise> findByUserId(String userId);
}
