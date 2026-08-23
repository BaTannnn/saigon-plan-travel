package com.saigonplantravel.backend.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.service.model.AuthenticationResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String DIFFERENT_SECRET = "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=";
    private static final String ISSUER = "saigon-plan-travel-test";
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, ISSUER, ACCESS_TOKEN_EXPIRATION));
    }

    @Test
    void generatesAndParsesValidAccessToken() {
        UUID publicId = UUID.randomUUID();
        AuthenticationResult authentication =
                new AuthenticationResult(publicId, "traveler@example.com", "Lan Anh", UserRole.USER);

        String token = jwtService.generateAccessToken(authentication);
        AccessTokenClaims claims = jwtService.parseAccessToken(token);

        assertThat(claims.userPublicId()).isEqualTo(publicId);
        assertThat(claims.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void returnsConfiguredAccessTokenExpirationSeconds() {
        assertThat(jwtService.getAccessTokenExpirationSeconds()).isEqualTo(1800L);
    }

    @Test
    void rejectsNullAccessToken() {
        assertThatThrownBy(() -> jwtService.parseAccessToken(null)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsBlankAccessToken() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("   ")).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsMalformedAccessToken() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = token(DIFFERENT_SECRET, ISSUER, UUID.randomUUID().toString(), "USER", futureExpiration());

        assertThatThrownBy(() -> jwtService.parseAccessToken(token)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        String token = token(SECRET, "different-issuer", UUID.randomUUID().toString(), "USER", futureExpiration());

        assertThatThrownBy(() -> jwtService.parseAccessToken(token)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token =
                token(SECRET, ISSUER, UUID.randomUUID().toString(), "USER", Date.from(Instant.EPOCH.plusSeconds(60)));

        assertThatThrownBy(() -> jwtService.parseAccessToken(token)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsTokenWithInvalidSubjectUuid() {
        String token = token(SECRET, ISSUER, "not-a-uuid", "USER", futureExpiration());

        assertThatThrownBy(() -> jwtService.parseAccessToken(token)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsTokenWithInvalidRole() {
        String token = token(SECRET, ISSUER, UUID.randomUUID().toString(), "SUPERUSER", futureExpiration());

        assertThatThrownBy(() -> jwtService.parseAccessToken(token)).isInstanceOf(InvalidAccessTokenException.class);
    }

    private static String token(String secret, String issuer, String subject, String role, Date expiration) {
        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("role", role)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    private static Date futureExpiration() {
        return Date.from(Instant.parse("2100-01-01T00:00:00Z"));
    }
}
