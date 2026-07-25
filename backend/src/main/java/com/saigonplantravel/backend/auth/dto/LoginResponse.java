package com.saigonplantravel.backend.auth.dto;

import com.saigonplantravel.backend.auth.domain.UserRole;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID publicId,
        String email,
        String displayName,
        UserRole role
) {
}