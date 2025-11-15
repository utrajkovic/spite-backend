package com.spite.backend.repository;

import com.spite.backend.model.TrainerClientLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TrainerClientRepository extends MongoRepository<TrainerClientLink, String> {
    List<TrainerClientLink> findByTrainerUsername(String trainerUsername);
    List<TrainerClientLink> findByClientUsername(String clientUsername);
    boolean existsByTrainerUsernameAndClientUsername(String trainerUsername, String clientUsername);
}
