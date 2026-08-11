package com.saigonplantravel.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequest(
        @NotBlank(message = "email is required")
                @Email(message = "email must be valid")
                @Size(max = 255, message = "email must contain at most 255 characters")
                String email,
        @NotBlank(message = "password is required")
                @Size(max = 64, message = "password must contain at most 64 characters")
                String password) {

    public LoginRequest {
        email = normalizeEmail(email);
    }

    private static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
