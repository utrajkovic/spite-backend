package com.spite.backend.repository;

import com.spite.backend.model.FcmToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends MongoRepository<FcmToken, String> {
    Optional<FcmToken> findByUsername(String username);
    List<FcmToken> findAllByUsername(String username);
    void deleteByUsername(String username);
}
