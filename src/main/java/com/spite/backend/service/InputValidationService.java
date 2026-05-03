package com.spite.backend.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

@Service
public class InputValidationService {

    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public boolean invalidUsername(String value) {
        if (isBlank(value)) {
            return true;
        }
        if (value.length() < 3 || value.length() > 32) {
            return true;
        }
        return !value.matches("^[a-zA-Z0-9._-]+$");
    }

    public boolean invalidPassword(String value) {
        return isBlank(value) || value.length() < 6 || value.length() > 128;
    }

    public boolean tooLong(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }

    public boolean nullOrEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}