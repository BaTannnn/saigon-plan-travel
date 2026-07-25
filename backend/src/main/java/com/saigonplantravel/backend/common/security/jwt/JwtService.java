package com.saigonplantravel.backend.common.security.jwt;

import com.saigonplantravel.backend.auth.service.model.AuthenticationResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        byte[] keyBytes = Decoders.BASE64.decode(
                jwtProperties.secret()
        );

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(
            AuthenticationResult authenticationResult
    ) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(
                jwtProperties.accessTokenExpiration()
        );

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(authenticationResult.publicId().toString())
                .claim(
                        "role",
                        authenticationResult.role().name()
                )
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties
                .accessTokenExpiration()
                .toSeconds();
    }
}