package com.saigonplantravel.backend.auth.service.model;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.entity.UserAccount;

import java.util.UUID;

public record AuthenticationResult(
        UUID publicId,
        String email,
        String displayName,
        UserRole role
) {

    public static AuthenticationResult from(UserAccount userAccount) {
        return new AuthenticationResult(
                userAccount.getPublicId(),
                userAccount.getEmail(),
                userAccount.getDisplayName(),
                userAccount.getRole()
        );
    }
}