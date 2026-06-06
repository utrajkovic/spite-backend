package com.spite.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.MealPlan;

public interface MealPlanRepository extends MongoRepository<MealPlan, String> {
    Optional<MealPlan> findByClientUsername(String clientUsername);
    void deleteByClientUsername(String clientUsername);
}
