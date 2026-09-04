package com.saigonplantravel.backend.auth.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration accessTokenExpiration) {

    public JwtProperties {
        if (accessTokenExpiration != null && (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative())) {
            throw new IllegalArgumentException("access token expiration must be positive");
        }
    }
}
