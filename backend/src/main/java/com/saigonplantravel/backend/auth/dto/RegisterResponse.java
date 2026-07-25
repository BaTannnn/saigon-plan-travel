package com.saigonplantravel.backend.auth.dto;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.entity.UserAccount;

import java.util.UUID;

public record RegisterResponse(
        UUID publicId,
        String email,
        String displayName,
        UserRole role
) {
    public static RegisterResponse from(UserAccount userAccount) {
        return new RegisterResponse(
                userAccount.getPublicId(),
                userAccount.getEmail(),
                userAccount.getDisplayName(),
                userAccount.getRole()
        );
    }
}
