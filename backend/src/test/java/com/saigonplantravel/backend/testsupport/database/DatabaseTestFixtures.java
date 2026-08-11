package com.saigonplantravel.backend.testsupport.database;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class DatabaseTestFixtures {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTestFixtures(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insertUser(
            String emailPrefix
    ) {
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
                emailPrefix
                        + "-"
                        + UUID.randomUUID()
                        + "@example.com",
                "integration-test-password",
                "Integration Test User"
        );
    }

    public Long insertCategory(
            String namePrefix,
            String slugPrefix
    ) {
        String uniqueSuffix =
                UUID.randomUUID().toString();

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO categories (
                    name,
                    slug
                )
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                namePrefix + " " + uniqueSuffix,
                slugPrefix + "-" + uniqueSuffix
        );
    }

    public TripFixture insertValidTrip(
            Long userId
    ) {
        UUID publicId =
                UUID.randomUUID();

        Long tripId =
                insertValidTrip(
                        publicId,
                        userId
                );

        return new TripFixture(
                tripId,
                publicId,
                userId
        );
    }

    public Long insertValidTrip(
            UUID publicId,
            Long userId
    ) {
        OffsetDateTime timestamp =
                OffsetDateTime.parse(
                        "2099-08-01T10:00:00+07:00"
                );

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
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?
                )
                RETURNING id
                """,
                Long.class,
                publicId,
                userId,
                LocalDate.of(2099, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                "Integration Test Location",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                "BALANCED",
                "MIXED",
                timestamp,
                timestamp
        );
    }
    public PlaceFixture insertValidPlace(
            String namePrefix,
            String slugPrefix,
            boolean indoor
    ) {
        String suffix =
                UUID.randomUUID().toString();

        String name =
                namePrefix + " " + suffix;

        String slug =
                slugPrefix + "-" + suffix;

        String address =
                "Test address, TP.HCM";

        BigDecimal latitude =
                new BigDecimal("10.7768890");

        BigDecimal longitude =
                new BigDecimal("106.7008060");

        int estimatedVisitMinutes = 90;

        BigDecimal minCost =
                new BigDecimal("50000.00");

        Long placeId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO places (
                            name,
                            slug,
                            address,
                            latitude,
                            longitude,
                            estimated_visit_minutes,
                            min_cost,
                            max_cost,
                            indoor,
                            active
                        )
                        VALUES (
                            ?, ?, ?, ?, ?,
                            ?, ?, ?, ?, TRUE
                        )
                        RETURNING id
                        """,
                        Long.class,
                        name,
                        slug,
                        address,
                        latitude,
                        longitude,
                        estimatedVisitMinutes,
                        minCost,
                        minCost,
                        indoor
                );

        return new PlaceFixture(
                placeId,
                name,
                slug,
                address,
                latitude,
                longitude,
                estimatedVisitMinutes,
                minCost,
                indoor
        );
    }
    public record PlaceFixture(
            Long placeId,
            String name,
            String slug,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int estimatedVisitMinutes,
            BigDecimal minCost,
            boolean indoor
    ) {
    }
    public record TripFixture(
            Long tripId,
            UUID tripPublicId,
            Long userId
    ) {
    }
}
