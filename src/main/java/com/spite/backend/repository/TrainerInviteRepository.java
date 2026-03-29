package com.spite.backend.repository;

import com.spite.backend.model.TrainerInvite;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TrainerInviteRepository extends MongoRepository<TrainerInvite, String> {
    List<TrainerInvite> findByClientUsernameAndStatus(String clientUsername, String status);
    List<TrainerInvite> findByTrainerUsernameAndStatus(String trainerUsername, String status);
    Optional<TrainerInvite> findByTrainerUsernameAndClientUsernameAndStatus(String trainerUsername, String clientUsername, String status);
    boolean existsByTrainerUsernameAndClientUsernameAndStatus(String trainerUsername, String clientUsername, String status);
}
