package com.saigonplantravel.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegisterRequest(
        @NotBlank(message = "email is required")
                @Email(message = "email must be valid")
                @Size(max = 255, message = "email must contain at most 255 characters")
                String email,
        @NotBlank(message = "password is required")
                @Size(min = 8, max = 64, message = "password must contain between 8 and 64 characters")
                String password,
        @NotBlank(message = "displayName is required")
                @Size(max = 100, message = "displayName must contain at most 100 characters")
                String displayName) {
    public RegisterRequest {
        email = normalizeEmail(email);
        displayName = normalizeDisplayName(displayName);
    }

    private static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeDisplayName(String value) {
        return value == null ? null : value.trim();
    }
}
