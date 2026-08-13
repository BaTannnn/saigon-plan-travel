package com.saigonplantravel.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationRequest;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.mapper.TripMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class TripServiceTransactionTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TripMapper tripMapper;

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.jpa.open-in-view", () -> "false");

        registry.add("app.security.jwt.secret", () -> "MDEyMzQ1Njc4OWFiY2RlZjAx" + "MjM0NTY3ODlhYmNkZWY=");
    }

    @Test
    void rollsBackEntireReplacementWhenResponseMappingFails() {
        Long userId = insertUser("test");

        UUID publicId = UUID.randomUUID();

        OffsetDateTime originalTimestamp = OffsetDateTime.parse("2099-08-01T10:00:00+07:00");

        Long tripId = insertOriginalTrip(publicId, userId, originalTimestamp);

        SaveTripRequest request = new SaveTripRequest(
                LocalDate.of(2099, 8, 25),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                new BigDecimal("700000.00"),
                new StartLocationRequest("Địa điểm mới", new BigDecimal("10.7798000"), new BigDecimal("106.6990000")),
                TravelPace.RELAXED,
                EnvironmentPreference.INDOOR);

        doThrow(new IllegalStateException("mapping failed")).when(tripMapper).toResponse(any(Trip.class));

        assertThatThrownBy(() -> tripService.replaceTrip(userId, publicId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mapping failed");

        String locationLabel = jdbcTemplate.queryForObject(
                """
                        SELECT start_location_label
                        FROM trips
                        WHERE id = ?
                        """,
                String.class,
                tripId);

        BigDecimal budget = jdbcTemplate.queryForObject(
                """
                        SELECT budget
                        FROM trips
                        WHERE id = ?
                        """,
                BigDecimal.class,
                tripId);

        OffsetDateTime updatedAt = jdbcTemplate.queryForObject(
                """
                        SELECT updated_at
                        FROM trips
                        WHERE id = ?
                        """,
                OffsetDateTime.class,
                tripId);

        assertThat(locationLabel).isEqualTo("Địa điểm ban đầu");

        assertThat(budget).isEqualByComparingTo("300000.00");

        assertThat(updatedAt).isEqualTo(originalTimestamp);

    }
    // Helper
    private Long insertUser(String emailPrefix) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (
                    public_id,
                    email,
                    password_hash,
                    display_name,
                    role,
                    active
                )
                VALUES (?, ?, ?, ?, 'USER', TRUE)
                RETURNING id
                """,
                Long.class,
                UUID.randomUUID(),
                emailPrefix + "-" + UUID.randomUUID() + "@example.com",
                "integration-test-password",
                "Trip Integration User");
    }

    private Long insertOriginalTrip(UUID publicId, Long userId, OffsetDateTime timestamp) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO trips (
                    public_id,
                    user_id,
                    trip_date,
                    start_time,
                    end_time,
                    budget,
                    start_location_label,
                    start_latitude,
                    start_longitude,
                    travel_pace,
                    environment_preference,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, ?, DATE '2099-08-20',
                    TIME '08:00', TIME '16:00',
                    300000.00,
                    'Địa điểm ban đầu',
                    10.7700000,
                    106.6900000,
                    'FAST',
                    'OUTDOOR',
                    ?, ?
                )
                RETURNING id
                """,
                Long.class,
                publicId,
                userId,
                timestamp,
                timestamp);
    }
}
