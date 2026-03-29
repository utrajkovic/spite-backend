package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("fcm_tokens")
public class FcmToken {

    @Id
    private String id;
    private String username;
    private String token;

    public FcmToken() {}

    public FcmToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
