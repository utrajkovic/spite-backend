package com.spite.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spite.backend.model.ShareInvite;

public interface ShareInviteRepository extends MongoRepository<ShareInvite, String> {
    List<ShareInvite> findByToUsernameAndStatus(String toUsername, String status);
    List<ShareInvite> findByFromUsernameAndStatus(String fromUsername, String status);
}
