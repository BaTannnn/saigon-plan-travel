package com.saigonplantravel.backend.scheduling.repository;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.ItineraryWarningCode;
import com.saigonplantravel.backend.scheduling.domain.SchedulingAlgorithmVersion;
import com.saigonplantravel.backend.scheduling.domain.SchedulingPolicy;
import com.saigonplantravel.backend.scheduling.domain.algorithm.GreedyItineraryScheduler;
import com.saigonplantravel.backend.scheduling.domain.algorithm.GreedySchedulingOutcome;
import com.saigonplantravel.backend.scheduling.domain.algorithm.SchedulingInput;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateRanker;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.domain.travel.HaversineDistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelTimeEstimator;
import com.saigonplantravel.backend.scheduling.domain.visit.PaceDurationPolicy;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityEvaluator;
import com.saigonplantravel.backend.scheduling.entity.Itinerary;
import com.saigonplantravel.backend.scheduling.entity.ItineraryItem;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures.PlaceFixture;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures.TripFixture;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class ItineraryRepositoryTest {

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
    private ItineraryRepository itineraryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndLoadsOwnedImmutableAggregate() {
        DatabaseTestFixtures fixtures = new DatabaseTestFixtures(jdbcTemplate);
        Long userId = fixtures.insertUser("itinerary-owner");
        Long anotherUserId = fixtures.insertUser("itinerary-other");
        TripFixture tripFixture = fixtures.insertValidTrip(userId);
        Long categoryId = fixtures.insertCategory(
                "Itinerary Repository Category",
                "itinerary-repository-category"
        );
        PlaceFixture place = fixtures.insertValidPlace(
                "Itinerary Repository Place",
                "itinerary-repository-place",
                true
        );

        SchedulingPolicy policy = new SchedulingPolicy(
                SchedulingAlgorithmVersion.GREEDY_V1,
                new BigDecimal("18.00"),
                5,
                100
        );
        TripSchedulingSnapshot trip = new TripSchedulingSnapshot(
                tripFixture.tripId(),
                tripFixture.tripPublicId(),
                LocalDate.of(2099, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                Set.of(categoryId),
                OffsetDateTime.parse("2099-08-01T10:00:00+07:00")
        );
        PlaceSchedulingCandidate candidate = new PlaceSchedulingCandidate(
                place.placeId(),
                place.name(),
                place.slug(),
                place.address(),
                null,
                null,
                place.latitude(),
                place.longitude(),
                place.estimatedVisitMinutes(),
                place.minCost(),
                place.indoor(),
                Set.of(categoryId),
                OpeningHoursSnapshot.unknown()
        );
        SchedulingInput input = new SchedulingInput(
                trip,
                List.of(candidate),
                policy,
                OffsetDateTime.parse("2099-08-01T11:00:00+07:00")
        );
        GreedyItineraryScheduler scheduler = new GreedyItineraryScheduler(
                new TravelTimeEstimator(new HaversineDistanceCalculator(), policy),
                new PaceDurationPolicy(),
                new VisitFeasibilityEvaluator(),
                new CandidateScorer(),
                new CandidateRanker()
        );
        GreedySchedulingOutcome outcome = scheduler.schedule(input);
        Itinerary itinerary = Itinerary.create(userId, input, outcome);

        Itinerary saved = itineraryRepository.saveAndFlush(itinerary);
        Long internalId = saved.getId();
        entityManager.clear();

        Itinerary loaded = itineraryRepository
                .findByPublicIdAndUserId(saved.getPublicId(), userId)
                .orElseThrow();

        assertThat(loaded.getTripPublicIdSnapshot())
                .isEqualTo(tripFixture.tripPublicId());
        assertThat(loaded.getPreferredCategoryIds()).containsExactly(categoryId);
        assertThat(loaded.getScheduledCount()).isEqualTo(1);
        assertThat(loaded.getTotalEstimatedCost()).isEqualByComparingTo("50000.00");
        assertThat(loaded.getItems()).hasSize(1);

        ItineraryItem loadedItem = loaded.getItems().getFirst();
        assertThat(loadedItem.getSequenceNo()).isEqualTo(1);
        assertThat(loadedItem.getPlaceAdministrativeUnitName()).isNull();
        assertThat(loadedItem.getPlaceAdministrativeUnitType()).isNull();
        assertThat(loaded.getWarnings())
                .extracting(warning -> warning.getCode())
                .containsExactly(
                        ItineraryWarningCode.TRAVEL_TIME_ESTIMATED,
                        ItineraryWarningCode.COST_USES_MINIMUM_ESTIMATE,
                        ItineraryWarningCode.OPENING_HOURS_UNKNOWN
                );
        assertThat(
                itineraryRepository.findByPublicIdAndUserId(
                        saved.getPublicId(),
                        anotherUserId
                )
        ).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM itinerary_items WHERE itinerary_id = ?",
                Integer.class,
                internalId
        )).isEqualTo(1);
    }
}
