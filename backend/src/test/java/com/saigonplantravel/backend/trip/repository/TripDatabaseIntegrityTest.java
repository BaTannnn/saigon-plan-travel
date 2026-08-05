package com.saigonplantravel.backend.trip.repository;

import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class TripDatabaseIntegrityTest {
    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName
                            .parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor(
                                    "postgres"
                            )
            );

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.jpa.open-in-view",
                () -> "false"
        );

        registry.add(
                "app.security.jwt.secret",
                () ->
                        "MDEyMzQ1Njc4OWFiY2RlZjAx"
                                + "MjM0NTY3ODlhYmNkZWY="
        );
    }
    private DatabaseTestFixtures fixtures;
    @BeforeEach
    void setUpFixtures() {
        fixtures =
                new DatabaseTestFixtures(
                        jdbcTemplate
                );
    }
    private static final LocalDate VALID_DATE =
            LocalDate.of(2099, 8, 20);

    private static final LocalTime VALID_START =
            LocalTime.of(8, 0);

    private static final LocalTime VALID_END =
            LocalTime.of(18, 0);

    private static final BigDecimal VALID_BUDGET =
            new BigDecimal("500000.00");

    private static final BigDecimal VALID_LATITUDE =
            new BigDecimal("10.7726400");

    private static final BigDecimal VALID_LONGITUDE =
            new BigDecimal("106.6980500");

    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.parse(
                    "2099-08-01T10:00:00+07:00"
            );

    private static final OffsetDateTime UPDATED_AT =
            OffsetDateTime.parse(
                    "2099-08-01T11:00:00+07:00"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsInvalidTripRowsWithNamedConstraints() {
        Long userId = fixtures.insertUser("constraint-user");

        assertInvalidTrip(
                userId,
                LocalTime.of(8, 0),
                LocalTime.of(8, 0),
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                LocalTime.of(8, 0),
                LocalTime.of(8, 59),
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                LocalTime.of(1, 0),
                LocalTime.of(20, 0),
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                new BigDecimal("-1.00"),
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                new BigDecimal("100000000.01"),
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                VALID_BUDGET,
                "   ",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                VALID_BUDGET,
                "Chợ Bến Thành",
                new BigDecimal("91.0000000"),
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                new BigDecimal("181.0000000"),
                "BALANCED",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "SLOW",
                "MIXED",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "ANY",
                CREATED_AT,
                UPDATED_AT
        );

        assertInvalidTrip(
                userId,
                VALID_START,
                VALID_END,
                VALID_BUDGET,
                "Chợ Bến Thành",
                VALID_LATITUDE,
                VALID_LONGITUDE,
                "BALANCED",
                "MIXED",
                UPDATED_AT,
                CREATED_AT
        );
    }
    @Test
    void rejectsDuplicateCategoryPreference() {
        Long userId = fixtures.insertUser("duplicate-preference");
        Long categoryId = fixtures.insertCategory(
                "Duplicate Preference Category",
                "duplicate-preference-category"
        );

        Long tripId = fixtures.insertValidTrip(
                UUID.randomUUID(),
                userId
        );

        jdbcTemplate.update(
                """
                INSERT INTO trip_category_preferences (
                    trip_id,
                    category_id
                )
                VALUES (?, ?)
                """,
                tripId,
                categoryId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO trip_category_preferences (
                    trip_id,
                    category_id
                )
                VALUES (?, ?)
                """,
                tripId,
                categoryId
        )).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
    @Test
    void enforcesTripForeignKeyDeleteBehavior() {
        Long userId = fixtures.insertUser("foreign-key-user");

        Long categoryId = fixtures.insertCategory(
                "Trip FK Category",
                "trip-fk-category"
        );

        Long tripId = fixtures.insertValidTrip(
                UUID.randomUUID(),
                userId
        );

        jdbcTemplate.update(
                """
                INSERT INTO trip_category_preferences (
                    trip_id,
                    category_id
                )
                VALUES (?, ?)
                """,
                tripId,
                categoryId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM categories WHERE id = ?",
                categoryId
        )).isInstanceOf(
                DataIntegrityViolationException.class
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM users WHERE id = ?",
                userId
        )).isInstanceOf(
                DataIntegrityViolationException.class
        );

        jdbcTemplate.update(
                "DELETE FROM trips WHERE id = ?",
                tripId
        );

        Integer preferenceCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM trip_category_preferences
                        WHERE trip_id = ?
                        """,
                        Integer.class,
                        tripId
                );

        assertThat(preferenceCount).isZero();
    }

    //Helper

    private Long insertTrip(
            UUID publicId,
            Long userId,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal budget,
            String locationLabel,
            BigDecimal latitude,
            BigDecimal longitude,
            String travelPace,
            String environmentPreference,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
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
                VALID_DATE,
                startTime,
                endTime,
                budget,
                locationLabel,
                latitude,
                longitude,
                travelPace,
                environmentPreference,
                createdAt,
                updatedAt
        );
    }

    private void assertInvalidTrip(
            Long userId,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal budget,
            String locationLabel,
            BigDecimal latitude,
            BigDecimal longitude,
            String travelPace,
            String environmentPreference,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        assertThatThrownBy(() -> insertTrip(
                UUID.randomUUID(),
                userId,
                startTime,
                endTime,
                budget,
                locationLabel,
                latitude,
                longitude,
                travelPace,
                environmentPreference,
                createdAt,
                updatedAt
        )).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
}