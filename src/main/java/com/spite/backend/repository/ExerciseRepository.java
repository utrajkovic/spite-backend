package com.spite.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.spite.backend.model.Exercise;

public interface ExerciseRepository extends MongoRepository<Exercise, String> {
}
