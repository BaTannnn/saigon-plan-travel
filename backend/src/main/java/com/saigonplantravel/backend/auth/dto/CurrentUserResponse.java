package com.saigonplantravel.backend.auth.dto;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.UserPrincipal;

import java.util.UUID;

public record CurrentUserResponse(
        UUID publicId,
        String email,
        String displayName,
        UserRole role
) {

    public static CurrentUserResponse from(
            UserPrincipal principal
    ) {
        return new CurrentUserResponse(
                principal.publicId(),
                principal.email(),
                principal.displayName(),
                principal.role()
        );
    }
}