package com.spite.backend.repository;

import com.spite.backend.model.ShareInvite;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ShareInviteRepository extends MongoRepository<ShareInvite, String> {
    List<ShareInvite> findByToUsernameAndStatus(String toUsername, String status);
    List<ShareInvite> findByFromUsernameAndStatus(String fromUsername, String status);
}
