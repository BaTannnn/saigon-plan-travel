package com.saigonplantravel.backend.common.security.jwt;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.service.model.AuthenticationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.issuer())
                .build();
    }

    public String generateAccessToken(AuthenticationResult authenticationResult) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(authenticationResult.publicId().toString())
                .claim("role", authenticationResult.role().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpiration().toSeconds();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidAccessTokenException();
        }

        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            String subject = claims.getSubject();
            String roleClaim = claims.get("role", String.class);

            if (subject == null
                    || subject.isBlank()
                    || roleClaim == null
                    || roleClaim.isBlank()
                    || claims.getExpiration() == null) {
                throw new InvalidAccessTokenException();
            }

            UUID userPublicId = UUID.fromString(subject);
            UserRole role = UserRole.valueOf(roleClaim);

            return new AccessTokenClaims(userPublicId, role);

        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAccessTokenException(exception);
        }
    }
}
