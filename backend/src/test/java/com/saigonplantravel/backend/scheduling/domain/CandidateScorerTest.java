package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScore;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScoringInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateScorerTest {

    private final CandidateScorer scorer =
            new CandidateScorer();

    @Test
    void calculatesGreedyV1ScoreComponents() {
        CandidateScoringInput input =
                new CandidateScoringInput(
                        2,
                        4,
                        new BigDecimal("1.000000"),
                        new BigDecimal("25.00"),
                        new BigDecimal("100.00"),
                        OpeningHoursStatus.KNOWN_OPEN,
                        10,
                        10,
                        80
                );

        CandidateScore score =
                scorer.score(input);

        assertThat(
                score.preferenceScore()
        ).isEqualByComparingTo(
                "20.00000000"
        );

        assertThat(
                score.distanceScore()
        ).isEqualByComparingTo(
                "12.50000000"
        );

        assertThat(
                score.costScore()
        ).isEqualByComparingTo(
                "11.25000000"
        );

        assertThat(
                score.openingConfidenceScore()
        ).isEqualByComparingTo(
                "10.00000000"
        );

        assertThat(
                score.timeEfficiencyScore()
        ).isEqualByComparingTo(
                "8.00000000"
        );

        assertThat(
                score.totalScore()
        ).isEqualByComparingTo(
                "61.75000000"
        );
    }
}