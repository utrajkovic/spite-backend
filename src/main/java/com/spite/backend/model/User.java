package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "users")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String password;
    private Role role = Role.USER;
    private boolean blocked = false;
    private String email;
    private boolean emailVerified = false;
    private boolean dailyReminderEnabled = true;
    private String dailyReminderTime = "07:00";

    public User() {
    }

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isDailyReminderEnabled() {
        return dailyReminderEnabled;
    }

    public void setDailyReminderEnabled(boolean dailyReminderEnabled) {
        this.dailyReminderEnabled = dailyReminderEnabled;
    }

    public String getDailyReminderTime() {
        return dailyReminderTime;
    }

    public void setDailyReminderTime(String dailyReminderTime) {
        this.dailyReminderTime = dailyReminderTime;
    }
}
