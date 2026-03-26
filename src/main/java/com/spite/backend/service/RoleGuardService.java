package com.spite.backend.service;

import com.spite.backend.model.Role;
import com.spite.backend.model.User;
import com.spite.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleGuardService {

    private final UserRepository userRepository;

    public RoleGuardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasRole(String username, Role requiredRole) {
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) return false;
        return u.getRole() == requiredRole;
    }
}
