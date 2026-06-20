package com.spite.backend.repository;

import com.spite.backend.model.DailyCheckIn;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DailyCheckInRepository extends MongoRepository<DailyCheckIn, String> {
    List<DailyCheckIn> findByUsernameOrderByCreatedAtDesc(String username);
    List<DailyCheckIn> findByTrainerUsernameOrderByCreatedAtDesc(String trainerUsername);
    List<DailyCheckIn> findByTrainerUsernameAndReviewedOrderByCreatedAtDesc(String trainerUsername, boolean reviewed);
    boolean existsByUsernameAndDateKey(String username, String dateKey);
    void deleteByUsername(String username);
}
