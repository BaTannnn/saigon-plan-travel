package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateRanker;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScore;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelEstimate;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitSchedule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateRankerTest {

    private final CandidateRanker ranker =
            new CandidateRanker();

    @Test
    void selectsBestCandidateUsingDeterministicTieBreakers() {
        EvaluatedCandidate higherScore =
                candidate(
                        10L,
                        "Z",
                        "100.00",
                        "5.000",
                        30,
                        "90.00000000"
                );

        EvaluatedCandidate lowerScore =
                candidate(
                        20L,
                        "A",
                        "0.00",
                        "0.100",
                        0,
                        "80.00000000"
                );

        assertWinner(
                higherScore,
                higherScore,
                lowerScore
        );

        EvaluatedCandidate lowerWaiting =
                candidate(
                        20L,
                        "B",
                        "100.00",
                        "2.000",
                        5,
                        "80.00000000"
                );

        EvaluatedCandidate higherWaiting =
                candidate(
                        10L,
                        "A",
                        "50.00",
                        "1.000",
                        20,
                        "80.00000000"
                );

        assertWinner(
                lowerWaiting,
                lowerWaiting,
                higherWaiting
        );

        EvaluatedCandidate shorterDistance =
                candidate(
                        20L,
                        "B",
                        "100.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        EvaluatedCandidate longerDistance =
                candidate(
                        10L,
                        "A",
                        "50.00",
                        "2.000",
                        5,
                        "80.00000000"
                );

        assertWinner(
                shorterDistance,
                shorterDistance,
                longerDistance
        );

        EvaluatedCandidate cheaper =
                candidate(
                        20L,
                        "B",
                        "50.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        EvaluatedCandidate moreExpensive =
                candidate(
                        10L,
                        "A",
                        "100.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        assertWinner(
                cheaper,
                cheaper,
                moreExpensive
        );

        EvaluatedCandidate alphabeticallyFirst =
                candidate(
                        20L,
                        "Bao tang A",
                        "50.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        EvaluatedCandidate alphabeticallyLater =
                candidate(
                        10L,
                        "Zoo",
                        "50.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        assertWinner(
                alphabeticallyFirst,
                alphabeticallyFirst,
                alphabeticallyLater
        );

        EvaluatedCandidate smallerPlaceId =
                candidate(
                        10L,
                        "Same",
                        "50.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        EvaluatedCandidate largerPlaceId =
                candidate(
                        20L,
                        "Same",
                        "50.00",
                        "1.000",
                        5,
                        "80.00000000"
                );

        assertWinner(
                smallerPlaceId,
                largerPlaceId,
                smallerPlaceId
        );
    }

    private void assertWinner(
            EvaluatedCandidate expected,
            EvaluatedCandidate first,
            EvaluatedCandidate second
    ) {
        EvaluatedCandidate winner =
                ranker.selectBest(
                                List.of(
                                        first,
                                        second
                                )
                        )
                        .orElseThrow();

        assertThat(winner)
                .isSameAs(expected);
    }

    private static EvaluatedCandidate candidate(
            Long placeId,
            String name,
            String minCost,
            String distanceKilometers,
            long waitingMinutes,
            String totalScore
    ) {
        PlaceSchedulingCandidate place =
                mock(
                        PlaceSchedulingCandidate.class
                );

        when(place.placeId())
                .thenReturn(placeId);

        when(place.name())
                .thenReturn(name);

        when(place.minCost())
                .thenReturn(
                        new BigDecimal(minCost)
                );

        TravelEstimate travel =
                new TravelEstimate(
                        new BigDecimal(
                                distanceKilometers
                        ),
                        10
                );

        LocalDateTime departureAt =
                LocalDateTime.of(
                        2026,
                        8,
                        9,
                        8,
                        0
                );

        LocalDateTime arrivalAt =
                departureAt.plusMinutes(
                        10
                );

        LocalDateTime visitStartAt =
                arrivalAt.plusMinutes(
                        waitingMinutes
                );

        VisitSchedule schedule =
                new VisitSchedule(
                        departureAt,
                        arrivalAt,
                        visitStartAt,
                        visitStartAt.plusMinutes(
                                60
                        )
                );

        BigDecimal zero =
                new BigDecimal(
                        "0.00000000"
                );

        CandidateScore score =
                new CandidateScore(
                        zero,
                        zero,
                        zero,
                        zero,
                        zero,
                        new BigDecimal(
                                totalScore
                        )
                );

        return new EvaluatedCandidate(
                place,
                travel,
                60,
                schedule,
                score
        );
    }
}