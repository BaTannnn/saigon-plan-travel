package com.saigonplantravel.backend.trip.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@Transactional
class TripRepositoryTest {

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

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndLoadsOwnedTrip() {
        Long ownerUserId = persistUser("owner@example.com");

        Long anotherUserId = persistUser("another@example.com");

        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-03T10:00:00+07:00");

        Trip trip = new Trip(
                ownerUserId,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                "Chợ Bến Thành",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                timestamp);

        Trip savedTrip = tripRepository.saveAndFlush(trip);

        UUID publicId = savedTrip.getPublicId();

        entityManager.clear();

        Trip loadedTrip =
                tripRepository.findByPublicIdAndUserId(publicId, ownerUserId).orElseThrow();

        assertThat(loadedTrip.getPublicId()).isEqualTo(publicId);

        assertThat(loadedTrip.getUserId()).isEqualTo(ownerUserId);

        assertThat(tripRepository.findByPublicIdAndUserId(publicId, anotherUserId))
                .isEmpty();
    }

    @Test
    void listsOnlyOwnedTripsInDeterministicOrder() {
        Long ownerUserId = persistUser("list-owner@example.com");
        Long anotherUserId = persistUser("list-another@example.com");
        Trip laterTrip =
                newTrip(ownerUserId, LocalDate.of(2026, 8, 21), LocalTime.of(9, 0), "Điểm xuất phát B");
        Trip tiedTripOne =
                newTrip(ownerUserId, LocalDate.of(2026, 8, 20), LocalTime.of(8, 0), "Điểm xuất phát A1");
        Trip tiedTripTwo =
                newTrip(ownerUserId, LocalDate.of(2026, 8, 20), LocalTime.of(8, 0), "Điểm xuất phát A2");
        Trip anotherUsersTrip = newTrip(
                anotherUserId, LocalDate.of(2026, 8, 19), LocalTime.of(7, 0), "Không thuộc chủ sở hữu");

        tripRepository.saveAllAndFlush(List.of(laterTrip, tiedTripOne, tiedTripTwo, anotherUsersTrip));

        List<UUID> expectedPublicIds = List.of(laterTrip, tiedTripOne, tiedTripTwo).stream()
                .sorted(Comparator.comparing(Trip::getTripDate)
                        .thenComparing(Trip::getStartTime)
                        .thenComparing(trip -> trip.getPublicId().toString()))
                .map(Trip::getPublicId)
                .toList();

        entityManager.clear();

        List<Trip> ownedTrips = tripRepository.findAllByUserIdOrderByTripDateAscStartTimeAscPublicIdAsc(ownerUserId);

        entityManager.clear();

        assertThat(ownedTrips)
                .extracting(Trip::getPublicId)
                .containsExactlyElementsOf(expectedPublicIds)
                .doesNotContain(anotherUsersTrip.getPublicId());
        assertThat(ownedTrips).allMatch(trip -> trip.getUserId().equals(ownerUserId));
    }

    @Test
    void replacesTripDetailsThroughDirtyChecking() {
        Long ownerUserId = persistUser("replace-owner@example.com");

        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T09:00:00+07:00");

        Trip trip = new Trip(
                ownerUserId,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                new BigDecimal("300000.00"),
                "Địa điểm cũ",
                new BigDecimal("10.7700000"),
                new BigDecimal("106.6900000"),
                TravelPace.FAST,
                EnvironmentPreference.OUTDOOR,
                createdAt);

        Trip savedTrip = tripRepository.saveAndFlush(trip);

        UUID publicId = savedTrip.getPublicId();
        entityManager.clear();

        Trip managedTrip =
                tripRepository.findByPublicIdAndUserId(publicId, ownerUserId).orElseThrow();

        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-08-03T10:00:00+07:00");

        managedTrip.replaceDetails(
                LocalDate.of(2026, 8, 25),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                new BigDecimal("700000.00"),
                "Địa điểm mới",
                new BigDecimal("10.7798000"),
                new BigDecimal("106.6990000"),
                TravelPace.RELAXED,
                EnvironmentPreference.INDOOR,
                updatedAt);

        /*
         * Không gọi tripRepository.save().
         * Entity đang managed nên Hibernate dùng dirty checking.
         */
        entityManager.flush();
        entityManager.clear();

        Trip reloadedTrip =
                tripRepository.findByPublicIdAndUserId(publicId, ownerUserId).orElseThrow();

        assertThat(reloadedTrip.getPublicId()).isEqualTo(publicId);

        assertThat(reloadedTrip.getUserId()).isEqualTo(ownerUserId);

        assertThat(reloadedTrip.getCreatedAt()).isEqualTo(createdAt);

        assertThat(reloadedTrip.getUpdatedAt()).isEqualTo(updatedAt);

        assertThat(reloadedTrip.getTripDate()).isEqualTo(LocalDate.of(2026, 8, 25));

        assertThat(reloadedTrip.getStartTime()).isEqualTo(LocalTime.of(9, 0));

        assertThat(reloadedTrip.getEndTime()).isEqualTo(LocalTime.of(17, 0));

        assertThat(reloadedTrip.getBudget()).isEqualByComparingTo("700000.00");

        assertThat(reloadedTrip.getStartLocationLabel()).isEqualTo("Địa điểm mới");

        assertThat(reloadedTrip.getTravelPace()).isEqualTo(TravelPace.RELAXED);

        assertThat(reloadedTrip.getEnvironmentPreference()).isEqualTo(EnvironmentPreference.INDOOR);

    }

    private Long persistUser(String email) {
        UserAccount user = new UserAccount(email, "test-password-hash", "Integration Test User");

        entityManager.persist(user);
        entityManager.flush();

        return user.getId();
    }

    private Trip newTrip(Long userId, LocalDate tripDate, LocalTime startTime, String startLocationLabel) {
        return new Trip(
                userId,
                tripDate,
                startTime,
                startTime.plusHours(8),
                new BigDecimal("500000.00"),
                startLocationLabel,
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                OffsetDateTime.parse("2026-08-03T10:00:00+07:00"));
    }
}
