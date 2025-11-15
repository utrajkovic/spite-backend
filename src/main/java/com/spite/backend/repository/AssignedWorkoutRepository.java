package com.spite.backend.repository;

import com.spite.backend.model.AssignedWorkout;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssignedWorkoutRepository extends MongoRepository<AssignedWorkout, String> {
    List<AssignedWorkout> findByClientUsername(String clientUsername);
    Optional<AssignedWorkout> findByClientUsernameAndWorkoutId(String clientUsername, String workoutId);
}
