package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "workout_feedback")
public class WorkoutFeedback {

    @Id
    private String id;

    private String workoutId;
    private String workoutTitle; // snapshot – čuva se u trenutku slanja
    private String userId;
    private long timestamp;
    private List<ExerciseFeedback> exercises;

    public WorkoutFeedback() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkoutId() { return workoutId; }
    public void setWorkoutId(String workoutId) { this.workoutId = workoutId; }

    public String getWorkoutTitle() { return workoutTitle; }
    public void setWorkoutTitle(String workoutTitle) { this.workoutTitle = workoutTitle; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<ExerciseFeedback> getExercises() { return exercises; }
    public void setExercises(List<ExerciseFeedback> exercises) { this.exercises = exercises; }
}
