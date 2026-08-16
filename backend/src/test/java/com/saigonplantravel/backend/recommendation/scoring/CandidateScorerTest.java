package com.saigonplantravel.backend.recommendation.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.ScoredCandidate;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

public class CandidateScorerTest {

    private final CandidateScorer scorer =
            new CandidateScorer(new TravelScorer(), new BudgetScorer(), new EnvironmentScorer());

    @Test
    void combinesCurrentCandidateSignalsIntoFinalScore() {

        Place place = place("museum", BigDecimal.ZERO, true);

        RecommendationCandidate candidate = new RecommendationCandidate(place, 0.90, "HIGHLIGHTS");

        Trip trip = tripAtSameLocation(new BigDecimal("500000"), EnvironmentPreference.INDOOR);

        ScoredCandidate result = scorer.score(candidate, trip, new BigDecimal("500000"), 2.0, 10);

        assertThat(result.semanticScore()).isEqualTo(0.90);

        assertThat(result.travelDistanceKm()).isEqualTo(2.0);

        assertThat(result.travelMinutes()).isEqualTo(10);

        assertThat(result.travelScore()).isEqualTo(0.75);

        assertThat(result.budgetScore()).isEqualTo(1.0);

        assertThat(result.environmentScore()).isEqualTo(1.0);

        assertThat(result.finalScore()).isCloseTo(0.895, within(0.000001));
    }

    @Test
    void rescoringSameCandidateChangesWithCurrentTravelTime() {

        Place place = place("museum", BigDecimal.ZERO, true);

        RecommendationCandidate candidate = new RecommendationCandidate(place, 0.90, "HIGHLIGHTS");

        Trip trip = tripAtSameLocation(new BigDecimal("500000"), EnvironmentPreference.INDOOR);

        ScoredCandidate near = scorer.score(candidate, trip, new BigDecimal("500000"), 1.0, 5);

        ScoredCandidate far = scorer.score(candidate, trip, new BigDecimal("500000"), 10.0, 60);

        assertThat(near.finalScore()).isGreaterThan(far.finalScore());
    }

    private Place place(String slug, BigDecimal minCost, boolean indoor) {

        return new Place(
                slug,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                90,
                minCost,
                minCost,
                indoor);
    }

    private Trip tripAtSameLocation(BigDecimal budget, EnvironmentPreference environmentPreference) {

        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                budget,
                "Test origin",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                TravelPace.BALANCED,
                environmentPreference,
                OffsetDateTime.parse("2026-08-15T10:00:00+07:00"));
    }
}
