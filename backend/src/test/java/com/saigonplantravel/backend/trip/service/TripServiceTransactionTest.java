package com.saigonplantravel.backend.trip.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationRequest;
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
@Testcontainers
@SpringBootTest
class TripServiceTransactionTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CategoryService categoryService;

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

    @Test
    void rollsBackEntireReplacementWhenPreferencePersistenceFails() {
        Long userId = insertUser("test");

        Long originalCategoryId = insertCategory(
                "Original Category",
                "original-category"
        );

        UUID publicId = UUID.randomUUID();

        OffsetDateTime originalTimestamp =
                OffsetDateTime.parse(
                        "2099-08-01T10:00:00+07:00"
                );

        Long tripId = insertOriginalTrip(
                publicId,
                userId,
                originalTimestamp
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
                originalCategoryId
        );

        SaveTripRequest request =
                new SaveTripRequest(
                        LocalDate.of(2099, 8, 25),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        new BigDecimal("700000.00"),
                        new StartLocationRequest(
                                "Địa điểm mới",
                                new BigDecimal("10.7798000"),
                                new BigDecimal("106.6990000")
                        ),
                        TravelPace.RELAXED,
                        EnvironmentPreference.INDOOR,
                        List.of("ghost-category")
                );

        when(
                categoryService.findCategoriesBySlugs(
                        request.categorySlugs()
                )
        ).thenReturn(
                List.of(
                        new CategoryResponse(
                                Long.MAX_VALUE,
                                "Ghost Category",
                                "ghost-category"
                        )
                )
        );

        assertThatThrownBy(() -> tripService.replaceTrip(
                userId,
                publicId,
                request
        )).hasRootCauseInstanceOf(SQLException.class);

        String locationLabel =
                jdbcTemplate.queryForObject(
                        """
                        SELECT start_location_label
                        FROM trips
                        WHERE id = ?
                        """,
                        String.class,
                        tripId
                );

        BigDecimal budget =
                jdbcTemplate.queryForObject(
                        """
                        SELECT budget
                        FROM trips
                        WHERE id = ?
                        """,
                        BigDecimal.class,
                        tripId
                );

        OffsetDateTime updatedAt =
                jdbcTemplate.queryForObject(
                        """
                        SELECT updated_at
                        FROM trips
                        WHERE id = ?
                        """,
                        OffsetDateTime.class,
                        tripId
                );

        List<Long> storedCategoryIds =
                jdbcTemplate.queryForList(
                        """
                        SELECT category_id
                        FROM trip_category_preferences
                        WHERE trip_id = ?
                        ORDER BY category_id
                        """,
                        Long.class,
                        tripId
                );

        assertThat(locationLabel)
                .isEqualTo("Địa điểm ban đầu");

        assertThat(budget)
                .isEqualByComparingTo("300000.00");

        assertThat(updatedAt)
                .isEqualTo(originalTimestamp);

        assertThat(storedCategoryIds)
                .containsExactly(originalCategoryId);
    }
    //Helper
    private Long insertCategory(
            String name,
            String slug
    ) {
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
                name + " " + UUID.randomUUID(),
                slug + "-" + UUID.randomUUID()
        );
    }
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
                emailPrefix
                        + "-"
                        + UUID.randomUUID()
                        + "@example.com",
                "integration-test-password",
                "Trip Integration User"
        );
    }
    private Long insertOriginalTrip(
            UUID publicId,
            Long userId,
            OffsetDateTime timestamp
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
                timestamp
        );
    }
}