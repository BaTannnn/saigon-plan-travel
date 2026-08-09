package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.scheduling.dto.ItineraryResponse;
import com.saigonplantravel.backend.scheduling.exception.ItineraryNotFoundException;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures.TripFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class SchedulingServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor("postgres")
            );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add(
                "app.security.jwt.secret",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generatesVersionsAndReadsImmutableSnapshotThroughRealModuleContracts() {
        DatabaseTestFixtures fixtures = new DatabaseTestFixtures(jdbcTemplate);
        Long ownerId = fixtures.insertUser("scheduling-flow-owner");
        Long otherUserId = fixtures.insertUser("scheduling-flow-other");
        TripFixture trip = fixtures.insertValidTrip(ownerId);
        Long cultureCategoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE slug = 'van-hoa'",
                Long.class
        );
        jdbcTemplate.update(
                "INSERT INTO trip_category_preferences (trip_id, category_id) VALUES (?, ?)",
                trip.tripId(),
                cultureCategoryId
        );

        ItineraryResponse first = schedulingService.generate(
                ownerId,
                trip.tripPublicId()
        );
        ItineraryResponse second = schedulingService.generate(
                ownerId,
                trip.tripPublicId()
        );

        assertThat(first.publicId()).isNotEqualTo(second.publicId());
        assertThat(first.items()).isNotEmpty();
        assertThat(first.items())
                .allSatisfy(item -> {
                    assertThat(item.place().administrativeUnitName()).isNull();
                    assertThat(item.place().administrativeUnitType()).isNull();
                });
        assertThat(first.summary().scheduledCount())
                .isEqualTo(first.items().size());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM itineraries WHERE trip_id = ?",
                Integer.class,
                trip.tripId()
        )).isEqualTo(2);

        ItineraryResponse stored = schedulingService.get(
                ownerId,
                first.publicId()
        );
        assertThat(stored.items()).isEqualTo(first.items());
        assertThat(stored.stale()).isFalse();

        jdbcTemplate.update(
                "UPDATE trips SET updated_at = ? WHERE id = ?",
                OffsetDateTime.parse("2099-08-01T10:01:00+07:00"),
                trip.tripId()
        );
        assertThat(schedulingService.get(ownerId, first.publicId()).stale())
                .isTrue();

        assertThatThrownBy(
                () -> schedulingService.get(otherUserId, first.publicId())
        ).isInstanceOf(ItineraryNotFoundException.class);
    }
}
