package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingQuery;
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
import com.saigonplantravel.backend.scheduling.dto.ItineraryResponse;
import com.saigonplantravel.backend.scheduling.entity.Itinerary;
import com.saigonplantravel.backend.scheduling.exception.NoFeasibleItineraryException;
import com.saigonplantravel.backend.scheduling.exception.SchedulingDataConflictException;
import com.saigonplantravel.backend.scheduling.mapper.ItineraryMapper;
import com.saigonplantravel.backend.scheduling.repository.ItineraryRepository;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.service.TripSchedulingQuery;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    private static final OffsetDateTime GENERATED_AT =
            OffsetDateTime.parse("2026-08-09T16:00:00+07:00");

    @Mock
    private TripSchedulingQuery tripSchedulingQuery;

    @Mock
    private PlaceSchedulingQuery placeSchedulingQuery;

    @Mock
    private ItineraryRepository itineraryRepository;

    private SchedulingPolicy policy;
    private GreedyItineraryScheduler scheduler;
    private SchedulingService service;

    @BeforeEach
    void setUp() {
        policy = new SchedulingPolicy(
                SchedulingAlgorithmVersion.GREEDY_V1,
                new BigDecimal("18.00"),
                5,
                100
        );
        scheduler = new GreedyItineraryScheduler(
                new TravelTimeEstimator(
                        new HaversineDistanceCalculator(),
                        policy
                ),
                new PaceDurationPolicy(),
                new VisitFeasibilityEvaluator(),
                new CandidateScorer(),
                new CandidateRanker()
        );
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-09T09:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
        service = new SchedulingService(
                tripSchedulingQuery,
                placeSchedulingQuery,
                scheduler,
                policy,
                itineraryRepository,
                new ItineraryMapper(),
                clock
        );
    }

    @Test
    void generatesPersistsAndMapsAnOwnedItinerary() {
        Long userId = 7L;
        TripSchedulingSnapshot trip = trip(EnvironmentPreference.MIXED);
        PlaceSchedulingCandidate candidate = candidate(true);

        when(tripSchedulingQuery.getByPublicId(trip.publicId(), userId))
                .thenReturn(trip);
        when(placeSchedulingQuery.findCandidates(
                trip.preferredCategoryIds(),
                trip.tripDate()
        )).thenReturn(List.of(candidate));
        when(itineraryRepository.save(any(Itinerary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ItineraryResponse response = service.generate(
                userId,
                trip.publicId()
        );

        assertThat(response.tripPublicId()).isEqualTo(trip.publicId());
        assertThat(response.generatedAt()).isEqualTo(GENERATED_AT);
        assertThat(response.stale()).isFalse();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().place().id())
                .isEqualTo(candidate.placeId());
        assertThat(response.summary().scheduledCount()).isEqualTo(1);
        assertThat(response.warnings()).hasSize(2);
        verify(itineraryRepository).save(any(Itinerary.class));
    }

    @Test
    void rejectsZeroFeasibleWithoutPersisting() {
        Long userId = 7L;
        TripSchedulingSnapshot trip = trip(EnvironmentPreference.OUTDOOR);

        when(tripSchedulingQuery.getByPublicId(trip.publicId(), userId))
                .thenReturn(trip);
        when(placeSchedulingQuery.findCandidates(
                trip.preferredCategoryIds(),
                trip.tripDate()
        )).thenReturn(List.of(candidate(true)));

        assertThatThrownBy(
                () -> service.generate(userId, trip.publicId())
        )
                .isInstanceOf(NoFeasibleItineraryException.class)
                .satisfies(exception -> {
                    NoFeasibleItineraryException noFeasible =
                            (NoFeasibleItineraryException) exception;
                    assertThat(noFeasible.getRejectionSummary().candidatePoolSize())
                            .isEqualTo(1);
                    assertThat(noFeasible.getRejectionSummary().rejectedByEnvironment())
                            .isEqualTo(1);
                });

        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void mapsInvalidPlaceSnapshotToDataConflictWithoutPersisting() {
        Long userId = 7L;
        TripSchedulingSnapshot trip = trip(EnvironmentPreference.MIXED);

        when(tripSchedulingQuery.getByPublicId(trip.publicId(), userId))
                .thenReturn(trip);
        when(placeSchedulingQuery.findCandidates(
                trip.preferredCategoryIds(),
                trip.tripDate()
        )).thenThrow(new IllegalStateException("unexpected projection row"));

        assertThatThrownBy(
                () -> service.generate(userId, trip.publicId())
        )
                .isInstanceOf(SchedulingDataConflictException.class)
                .hasMessage("Place scheduling snapshot is inconsistent")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void getsStoredSnapshotAndCalculatesStaleFromCurrentTrip() {
        Long userId = 7L;
        TripSchedulingSnapshot originalTrip = trip(EnvironmentPreference.MIXED);
        PlaceSchedulingCandidate candidate = candidate(true);
        SchedulingInput input = new SchedulingInput(
                originalTrip,
                List.of(candidate),
                policy,
                GENERATED_AT
        );
        GreedySchedulingOutcome outcome = scheduler.schedule(input);
        Itinerary itinerary = Itinerary.create(userId, input, outcome);
        TripSchedulingSnapshot updatedTrip = new TripSchedulingSnapshot(
                originalTrip.tripId(),
                originalTrip.publicId(),
                originalTrip.tripDate(),
                originalTrip.startTime(),
                originalTrip.endTime(),
                originalTrip.budget(),
                originalTrip.startLatitude(),
                originalTrip.startLongitude(),
                originalTrip.travelPace(),
                originalTrip.environmentPreference(),
                originalTrip.preferredCategoryIds(),
                originalTrip.updatedAt().plusMinutes(1)
        );

        when(itineraryRepository.findByPublicIdAndUserId(
                itinerary.getPublicId(),
                userId
        )).thenReturn(Optional.of(itinerary));
        when(tripSchedulingQuery.getByPublicId(
                originalTrip.publicId(),
                userId
        )).thenReturn(updatedTrip);

        ItineraryResponse response = service.get(
                userId,
                itinerary.getPublicId()
        );

        assertThat(response.stale()).isTrue();
        assertThat(response.tripWindow().tripDate())
                .isEqualTo(originalTrip.tripDate());
    }

    private TripSchedulingSnapshot trip(
            EnvironmentPreference environmentPreference
    ) {
        return new TripSchedulingSnapshot(
                11L,
                UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98"),
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                environmentPreference,
                Set.of(3L, 1L),
                OffsetDateTime.parse("2026-08-08T15:00:00+07:00")
        );
    }

    private PlaceSchedulingCandidate candidate(boolean indoor) {
        return new PlaceSchedulingCandidate(
                21L,
                "Địa điểm demo",
                "dia-diem-demo",
                "Địa chỉ demo, TP.HCM",
                null,
                null,
                new BigDecimal("10.7768890"),
                new BigDecimal("106.7008060"),
                90,
                new BigDecimal("50000.00"),
                indoor,
                Set.of(1L, 3L),
                OpeningHoursSnapshot.knownOpen(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0)
                )
        );
    }
}
