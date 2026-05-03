package com.spite.backend.repository;

import com.spite.backend.model.VideoComment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VideoCommentRepository extends MongoRepository<VideoComment, String> {
    List<VideoComment> findByFeedbackIdOrderByTimestampSecAscCreatedAtAsc(String feedbackId);
}
