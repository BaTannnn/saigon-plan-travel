package com.saigonplantravel.backend.recommendation.scoring;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DistanceScorerTest {

    private final DistanceScorer scorer = new DistanceScorer();

    @Test
    void returnsOneForZeroDistance() {
        assertThat(scorer.score(0.0)).isEqualTo(1.0);
    }

    @Test
    void returnsHalfAtReferenceDistance() {
        assertThat(scorer.score(5.0)).isEqualTo(0.5);
    }

    @Test
    void scoreDecreasesAsDistanceIncreases() {
        double near = scorer.score(1.0);
        double medium = scorer.score(5.0);
        double far = scorer.score(10.0);

        assertThat(near).isGreaterThan(medium);

        assertThat(medium).isGreaterThan(far);
    }

    @Test
    void rejectsNegativeDistance() {
        assertThatThrownBy(() -> scorer.score(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("distanceKm must not be negative");
    }

    @Test
    void producesExpectedDecay() {
        assertThat(scorer.score(1.0)).isCloseTo(0.8333, within(0.0001));

        assertThat(scorer.score(10.0)).isCloseTo(0.3333, within(0.0001));
    }
}
