package com.saigonplantravel.backend.scheduling.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class TravelScorerTest {

    private final TravelScorer scorer = new TravelScorer();

    @Test
    void returnsOneForZeroTravelTime() {

        assertThat(scorer.score(0)).isEqualTo(1.0);
    }

    @Test
    void returnsHalfAtReferenceTravelTime() {

        assertThat(scorer.score(30)).isEqualTo(0.5);
    }

    @Test
    void scoreDecreasesAsTravelTimeIncreases() {

        double shortTravel = scorer.score(10);

        double mediumTravel = scorer.score(30);

        double longTravel = scorer.score(60);

        assertThat(shortTravel).isGreaterThan(mediumTravel);

        assertThat(mediumTravel).isGreaterThan(longTravel);
    }

    @Test
    void producesExpectedTravelScores() {

        assertThat(scorer.score(10)).isCloseTo(0.75, within(0.0001));

        assertThat(scorer.score(60)).isCloseTo(0.3333, within(0.0001));
    }

    @Test
    void rejectsNegativeTravelTime() {

        assertThatThrownBy(() -> scorer.score(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("estimatedMinutes must not be negative");
    }
}
