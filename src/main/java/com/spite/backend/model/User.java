package com.spite.backend.model;

import java.util.ArrayList;
import java.util.List;

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
    private String avatarUrl;

    // Korisnički (CLIENT) podsetnik za trening
    private boolean clientReminderEnabled = false;
    private String clientReminderMode = "SESSIONS"; // SESSIONS | CUSTOM
    private String clientReminderTime = "07:00";    // vreme slanja za SESSIONS režim
    private List<WorkoutReminder> customReminders = new ArrayList<>();

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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isClientReminderEnabled() {
        return clientReminderEnabled;
    }

    public void setClientReminderEnabled(boolean clientReminderEnabled) {
        this.clientReminderEnabled = clientReminderEnabled;
    }

    public String getClientReminderMode() {
        return clientReminderMode;
    }

    public void setClientReminderMode(String clientReminderMode) {
        this.clientReminderMode = clientReminderMode;
    }

    public String getClientReminderTime() {
        return clientReminderTime;
    }

    public void setClientReminderTime(String clientReminderTime) {
        this.clientReminderTime = clientReminderTime;
    }

    public List<WorkoutReminder> getCustomReminders() {
        return customReminders;
    }

    public void setCustomReminders(List<WorkoutReminder> customReminders) {
        this.customReminders = customReminders;
    }
}
