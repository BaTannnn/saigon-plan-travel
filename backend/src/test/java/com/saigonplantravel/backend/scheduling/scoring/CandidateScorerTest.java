package com.saigonplantravel.backend.scheduling.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScoredCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CandidateScorerTest {

    private final CandidateScorer scorer =
            new CandidateScorer(new TravelScorer(), new BudgetScorer(), new EnvironmentScorer());

    @Test
    void combinesCurrentCandidateSignalsIntoFinalScore() {
        SchedulingCandidate candidate = candidate(BigDecimal.ZERO, true, 0.90);
        PlanningContext context = context(EnvironmentPreference.INDOOR);

        ScoredCandidate result = scorer.score(candidate, context, new BigDecimal("500000"), 2.0, 10);

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
        SchedulingCandidate candidate = candidate(BigDecimal.ZERO, true, 0.90);
        PlanningContext context = context(EnvironmentPreference.INDOOR);

        ScoredCandidate near = scorer.score(candidate, context, new BigDecimal("500000"), 1.0, 5);
        ScoredCandidate far = scorer.score(candidate, context, new BigDecimal("500000"), 10.0, 60);

        assertThat(near.finalScore()).isGreaterThan(far.finalScore());
    }

    private SchedulingCandidate candidate(BigDecimal estimatedCost, boolean indoor, double semanticScore) {
        SchedulingPlace place = new SchedulingPlace(
                "museum",
                "Museum",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                90,
                estimatedCost,
                indoor,
                null,
                OpeningWindow.open(LocalTime.of(8, 0), LocalTime.of(18, 0)));
        return new SchedulingCandidate(place, semanticScore, "HIGHLIGHTS");
    }

    private PlanningContext context(EnvironmentPreference environmentPreference) {
        return new PlanningContext(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000"),
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                environmentPreference);
    }
}
