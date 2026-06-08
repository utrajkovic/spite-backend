package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.OfflineClient;

public interface OfflineClientRepository extends MongoRepository<OfflineClient, String> {
    List<OfflineClient> findByTrainerUsername(String trainerUsername);
}
