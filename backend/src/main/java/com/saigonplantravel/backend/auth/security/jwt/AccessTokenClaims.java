package com.saigonplantravel.backend.auth.security.jwt;

import com.saigonplantravel.backend.auth.domain.UserRole;
import java.util.UUID;

public record AccessTokenClaims(UUID userPublicId, UserRole role) {}
