package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("completed_workouts")
public class CompletedWorkout {

    @Id
    private String id;

    private String username;
    private String workoutId;
    private String workoutTitle;
    private long completedAt;
    private boolean hasFeedback = false;
    private String feedbackId;

    public CompletedWorkout() {}

    public CompletedWorkout(String username, String workoutId, String workoutTitle, long completedAt) {
        this.username = username;
        this.workoutId = workoutId;
        this.workoutTitle = workoutTitle;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getWorkoutId() { return workoutId; }
    public void setWorkoutId(String workoutId) { this.workoutId = workoutId; }

    public String getWorkoutTitle() { return workoutTitle; }
    public void setWorkoutTitle(String workoutTitle) { this.workoutTitle = workoutTitle; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public boolean isHasFeedback() { return hasFeedback; }
    public void setHasFeedback(boolean hasFeedback) { this.hasFeedback = hasFeedback; }

    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
}
