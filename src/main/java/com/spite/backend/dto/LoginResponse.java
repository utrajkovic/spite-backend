package com.spite.backend.dto;

import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.model.UserSession;

public class LoginResponse {

    private String token;
    private long expiresAt;
    private UserPayload user;

    public static LoginResponse from(User user, UserSession session) {
        LoginResponse response = new LoginResponse();
        response.setToken(session.getToken());
        response.setExpiresAt(session.getExpiresAt());
        UserPayload payload = new UserPayload(user.getId(), user.getUsername(), user.getRole(), user.isBlocked());
        payload.setFullName(user.getFullName());
        response.setUser(payload);
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserPayload getUser() {
        return user;
    }

    public void setUser(UserPayload user) {
        this.user = user;
    }

    public static class UserPayload {
        private String id;
        private String username;
        private String fullName;
        private Role role;
        private boolean blocked;

        public UserPayload() {
        }

        public UserPayload(String id, String username, Role role, boolean blocked) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.blocked = blocked;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
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
    }
}