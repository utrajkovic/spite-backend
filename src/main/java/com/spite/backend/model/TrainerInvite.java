package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("trainer_invites")
public class TrainerInvite {

    @Id
    private String id;

    private String trainerUsername;
    private String clientUsername;
    private String status; // "PENDING", "ACCEPTED", "DECLINED"
    private Instant createdAt = Instant.now();

    public TrainerInvite() {}

    public TrainerInvite(String trainerUsername, String clientUsername) {
        this.trainerUsername = trainerUsername;
        this.clientUsername = clientUsername;
        this.status = "PENDING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrainerUsername() { return trainerUsername; }
    public void setTrainerUsername(String trainerUsername) { this.trainerUsername = trainerUsername; }

    public String getClientUsername() { return clientUsername; }
    public void setClientUsername(String clientUsername) { this.clientUsername = clientUsername; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
