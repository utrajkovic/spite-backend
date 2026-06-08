package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.OfflineClientLog;

public interface OfflineClientLogRepository extends MongoRepository<OfflineClientLog, String> {
    List<OfflineClientLog> findByOfflineClientIdOrderByDateDesc(String offlineClientId);
    void deleteByOfflineClientId(String offlineClientId);
}
