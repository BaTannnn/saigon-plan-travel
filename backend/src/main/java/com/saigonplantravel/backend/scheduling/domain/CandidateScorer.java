package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class CandidateScorer {

    private static final int SCORE_SCALE =
            8;

    private static final int DISTANCE_SCALE =
            6;

    private static final RoundingMode ROUNDING_MODE =
            RoundingMode.HALF_UP;

    private static final BigDecimal PREFERENCE_WEIGHT =
            BigDecimal.valueOf(40);

    private static final BigDecimal DISTANCE_WEIGHT =
            BigDecimal.valueOf(25);

    private static final BigDecimal COST_WEIGHT =
            BigDecimal.valueOf(15);

    private static final BigDecimal OPENING_CONFIDENCE_WEIGHT =
            BigDecimal.valueOf(10);

    private static final BigDecimal TIME_EFFICIENCY_WEIGHT =
            BigDecimal.valueOf(10);

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    private static final BigDecimal ZERO_SCORE =
            BigDecimal.ZERO.setScale(
                    SCORE_SCALE
            );

    public CandidateScore score(
            CandidateScoringInput input
    ) {
        Objects.requireNonNull(
                input,
                "input must not be null"
        );

        BigDecimal preferenceScore =
                calculatePreferenceScore(
                        input
                );

        BigDecimal distanceScore =
                calculateDistanceScore(
                        input
                );

        BigDecimal costScore =
                calculateCostScore(
                        input
                );

        BigDecimal openingConfidenceScore =
                calculateOpeningConfidenceScore(
                        input
                );

        BigDecimal timeEfficiencyScore =
                calculateTimeEfficiencyScore(
                        input
                );

        BigDecimal totalScore =
                preferenceScore
                        .add(distanceScore)
                        .add(costScore)
                        .add(openingConfidenceScore)
                        .add(timeEfficiencyScore)
                        .setScale(
                                SCORE_SCALE,
                                ROUNDING_MODE
                        );

        return new CandidateScore(
                preferenceScore,
                distanceScore,
                costScore,
                openingConfidenceScore,
                timeEfficiencyScore,
                totalScore
        );
    }

    private static BigDecimal calculatePreferenceScore(
            CandidateScoringInput input
    ) {
        BigDecimal matchRatio =
                BigDecimal
                        .valueOf(
                                input.matchedPreferredCategoryCount()
                        )
                        .divide(
                                BigDecimal.valueOf(
                                        input.totalPreferredCategoryCount()
                                ),
                                SCORE_SCALE,
                                ROUNDING_MODE
                        );

        return PREFERENCE_WEIGHT
                .multiply(matchRatio)
                .setScale(
                        SCORE_SCALE,
                        ROUNDING_MODE
                );
    }

    private static BigDecimal calculateDistanceScore(
            CandidateScoringInput input
    ) {
        BigDecimal distance =
                input.distanceKilometers()
                        .setScale(
                                DISTANCE_SCALE,
                                ROUNDING_MODE
                        );

        BigDecimal denominator =
                ONE.add(
                        distance
                );

        return DISTANCE_WEIGHT
                .divide(
                        denominator,
                        SCORE_SCALE,
                        ROUNDING_MODE
                );
    }

    private static BigDecimal calculateCostScore(
            CandidateScoringInput input
    ) {
        if (input.remainingBudget()
                .signum() == 0) {
            return COST_WEIGHT.setScale(
                    SCORE_SCALE
            );
        }

        BigDecimal costRatio =
                input.minCost()
                        .divide(
                                input.remainingBudget(),
                                SCORE_SCALE,
                                ROUNDING_MODE
                        );

        BigDecimal cappedCostRatio =
                costRatio.min(
                        ONE
                );

        return COST_WEIGHT
                .multiply(
                        ONE.subtract(
                                cappedCostRatio
                        )
                )
                .setScale(
                        SCORE_SCALE,
                        ROUNDING_MODE
                );
    }

    private static BigDecimal
    calculateOpeningConfidenceScore(
            CandidateScoringInput input
    ) {
        if (input.openingHoursStatus()
                == OpeningHoursStatus.KNOWN_OPEN) {
            return OPENING_CONFIDENCE_WEIGHT
                    .setScale(
                            SCORE_SCALE
                    );
        }

        return ZERO_SCORE;
    }

    private static BigDecimal
    calculateTimeEfficiencyScore(
            CandidateScoringInput input
    ) {
        long totalMinutes =
                (long) input.travelMinutes()
                        + input.waitingMinutes()
                        + input.adjustedVisitMinutes();

        BigDecimal efficiencyRatio =
                BigDecimal
                        .valueOf(
                                input.adjustedVisitMinutes()
                        )
                        .divide(
                                BigDecimal.valueOf(
                                        totalMinutes
                                ),
                                SCORE_SCALE,
                                ROUNDING_MODE
                        );

        return TIME_EFFICIENCY_WEIGHT
                .multiply(
                        efficiencyRatio
                )
                .setScale(
                        SCORE_SCALE,
                        ROUNDING_MODE
                );
    }
}