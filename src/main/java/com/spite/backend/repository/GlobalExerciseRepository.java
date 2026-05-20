package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.GlobalExercise;

public interface GlobalExerciseRepository extends MongoRepository<GlobalExercise, String> {
    List<GlobalExercise> findByMuscleGroupsIn(List<String> muscleGroups);
    List<GlobalExercise> findAllByOrderBySortOrderAsc();
}