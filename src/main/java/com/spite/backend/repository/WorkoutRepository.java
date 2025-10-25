package com.spite.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.spite.backend.model.Workout;

public interface WorkoutRepository extends MongoRepository<Workout, String> {
}

