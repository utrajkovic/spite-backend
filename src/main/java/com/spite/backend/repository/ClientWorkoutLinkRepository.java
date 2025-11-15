package com.spite.backend.repository;

import com.spite.backend.model.ClientWorkoutLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ClientWorkoutLinkRepository extends MongoRepository<ClientWorkoutLink, String> {
    List<ClientWorkoutLink> findByClientUsername(String clientUsername);
    Optional<ClientWorkoutLink> findByClientUsernameAndWorkoutId(String clientUsername, String workoutId);
}
