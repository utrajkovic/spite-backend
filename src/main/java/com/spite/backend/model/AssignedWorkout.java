package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("assigned_workouts")
public class AssignedWorkout {
    @Id
    private String id;

    private String workoutId;         
    private String clientUsername;    
    private String assignedBy;      
    private Instant createdAt = Instant.now();

    public AssignedWorkout() {}

    public AssignedWorkout(String workoutId, String clientUsername, String assignedBy) {
        this.workoutId = workoutId;
        this.clientUsername = clientUsername;
        this.assignedBy = assignedBy;
    }

    public String getId() { return id; }
    public String getWorkoutId() { return workoutId; }
    public void setWorkoutId(String workoutId) { this.workoutId = workoutId; }

    public String getClientUsername() { return clientUsername; }
    public void setClientUsername(String clientUsername) { this.clientUsername = clientUsername; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
