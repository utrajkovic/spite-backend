package com.spite.backend.repository;

import com.spite.backend.model.CompletedWorkout;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CompletedWorkoutRepository extends MongoRepository<CompletedWorkout, String> {
    List<CompletedWorkout> findByUsername(String username);
    void deleteByUsername(String username);
}
