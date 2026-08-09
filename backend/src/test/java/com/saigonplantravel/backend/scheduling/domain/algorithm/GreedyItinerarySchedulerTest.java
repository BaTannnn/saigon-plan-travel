package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.SchedulingAlgorithmVersion;
import com.saigonplantravel.backend.scheduling.domain.SchedulingPolicy;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateRanker;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.scheduling.domain.travel.HaversineDistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelTimeEstimator;
import com.saigonplantravel.backend.scheduling.domain.visit.PaceDurationPolicy;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityEvaluator;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GreedyItinerarySchedulerTest {

    private final SchedulingPolicy policy =
            new SchedulingPolicy(
                    SchedulingAlgorithmVersion.GREEDY_V1,
                    new BigDecimal("18.00"),
                    5,
                    100
            );

    private final GreedyItineraryScheduler scheduler =
            new GreedyItineraryScheduler(
                    new TravelTimeEstimator(
                            new HaversineDistanceCalculator(),
                            policy
                    ),
                    new PaceDurationPolicy(),
                    new VisitFeasibilityEvaluator(),
                    new CandidateScorer(),
                    new CandidateRanker()
            );

    @Test
    void recomputesRemainingCandidatesFromUpdatedStateAfterEachSelection() {
        TripSchedulingSnapshot trip =
                new TripSchedulingSnapshot(
                        1L,
                        UUID.fromString(
                                "11111111-1111-1111-1111-111111111111"
                        ),
                        LocalDate.of(
                                2026,
                                8,
                                9
                        ),
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0),
                        new BigDecimal("1000.00"),
                        new BigDecimal("10.0000000"),
                        new BigDecimal("106.0000000"),
                        TravelPace.BALANCED,
                        EnvironmentPreference.MIXED,
                        Set.of(
                                1L,
                                2L
                        ),
                        OffsetDateTime.parse(
                                "2026-08-08T15:00:00+07:00"
                        )
                );

        /*
         * A match cả 2 preferred categories,
         * nên đủ lợi thế để được chọn đầu tiên.
         */
        PlaceSchedulingCandidate placeA =
                candidate(
                        10L,
                        "Place A",
                        "10.0000000",
                        "106.0100000",
                        Set.of(
                                1L,
                                2L
                        )
                );

        /*
         * B xa Origin hơn C,
         * nhưng cực gần A.
         */
        PlaceSchedulingCandidate placeB =
                candidate(
                        20L,
                        "Place B",
                        "10.0000000",
                        "106.0110000",
                        Set.of(1L)
                );

        /*
         * C gần Origin hơn B,
         * nhưng xa A hơn B.
         */
        PlaceSchedulingCandidate placeC =
                candidate(
                        30L,
                        "Place C",
                        "10.0000000",
                        "106.0050000",
                        Set.of(1L)
                );

        SchedulingInput input =
                new SchedulingInput(
                        trip,

                        /*
                         * Cố tình truyền thứ tự lộn xộn.
                         */
                        List.of(
                                placeC,
                                placeB,
                                placeA
                        ),
                        policy,
                        OffsetDateTime.parse(
                                "2026-08-08T16:00:00+07:00"
                        )
                );

        GreedySchedulingOutcome outcome =
                scheduler.schedule(
                        input
                );

        assertThat(
                outcome.scheduledCandidates()
        )
                .extracting(
                        evaluated ->
                                evaluated
                                        .candidate()
                                        .placeId()
                )
                .containsExactly(
                        10L,
                        20L,
                        30L
                );

        EvaluatedCandidate second =
                outcome
                        .scheduledCandidates()
                        .get(1);

        assertThat(
                second
                        .travelEstimate()
                        .travelMinutes()
        ).isEqualTo(
                6
        );

        assertThat(
                second
                        .travelEstimate()
                        .distanceKilometers()
        ).isLessThan(
                new BigDecimal("0.200")
        );
    }

    private static PlaceSchedulingCandidate candidate(
            Long placeId,
            String name,
            String latitude,
            String longitude,
            Set<Long> matchedCategoryIds
    ) {
        return new PlaceSchedulingCandidate(
                placeId,
                name,
                "place-" + placeId,
                "Địa chỉ " + placeId,
                "Bến Nghé",
                AdministrativeUnitType.WARD,
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                60,
                new BigDecimal("10.00"),
                true,
                matchedCategoryIds,
                OpeningHoursSnapshot.knownOpen(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0)
                )
        );
    }
}