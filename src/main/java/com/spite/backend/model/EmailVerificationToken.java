package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("email_verification_tokens")
public class EmailVerificationToken {

    @Id
    private String id;

    @Indexed
    private String username;

    @Indexed(unique = true)
    private String token;

    private String email;
    private long expiresAt;

    public EmailVerificationToken() {}

    public EmailVerificationToken(String username, String token, String email, long expiresAt) {
        this.username = username;
        this.token = token;
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}
