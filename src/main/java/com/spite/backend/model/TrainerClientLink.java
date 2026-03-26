package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trainer_client_links")
public class TrainerClientLink {
    @Id
    private String id;

    private String trainerUsername;
    private String clientUsername;

    public TrainerClientLink() {
    }

    public TrainerClientLink(String trainerUsername, String clientUsername) {
        this.trainerUsername = trainerUsername;
        this.clientUsername = clientUsername;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrainerUsername() {
        return trainerUsername;
    }

    public void setTrainerUsername(String trainerUsername) {
        this.trainerUsername = trainerUsername;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }
}
