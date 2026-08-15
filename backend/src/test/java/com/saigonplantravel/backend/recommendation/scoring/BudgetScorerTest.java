package com.saigonplantravel.backend.recommendation.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BudgetScorerTest {

    private final BudgetScorer scorer = new BudgetScorer();

    @Test
    void returnsOneForFreePlace() {

        double score = scorer.score(BigDecimal.ZERO, new BigDecimal("500000"));

        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void returnsHalfWhenMinimumCostEqualsBudget() {

        double score = scorer.score(new BigDecimal("500000"), new BigDecimal("500000"));

        assertThat(score).isEqualTo(0.5);
    }

    @Test
    void cheaperPlaceReceivesHigherScore() {

        double cheap = scorer.score(new BigDecimal("100000"), new BigDecimal("500000"));

        double expensive = scorer.score(new BigDecimal("400000"), new BigDecimal("500000"));

        assertThat(cheap).isGreaterThan(expensive);
    }

    @Test
    void returnsOneForFreePlaceWithZeroBudget() {

        double score = scorer.score(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void returnsZeroForPaidPlaceWithZeroBudget() {

        double score = scorer.score(new BigDecimal("10000"), BigDecimal.ZERO);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void producesExpectedScoreForHalfBudget() {

        double score = scorer.score(new BigDecimal("250000"), new BigDecimal("500000"));

        assertThat(score).isCloseTo(0.6667, within(0.0001));
    }

    @Test
    void rejectsNegativeCost() {

        assertThatThrownBy(() -> scorer.score(new BigDecimal("-1"), new BigDecimal("500000")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
