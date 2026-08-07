package com.saigonplantravel.backend.scheduling.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CandidateScore(
        BigDecimal preferenceScore,
        BigDecimal distanceScore,
        BigDecimal costScore,
        BigDecimal openingConfidenceScore,
        BigDecimal timeEfficiencyScore,
        BigDecimal totalScore
) {

    public CandidateScore {
        Objects.requireNonNull(
                preferenceScore,
                "preferenceScore must not be null"
        );

        Objects.requireNonNull(
                distanceScore,
                "distanceScore must not be null"
        );

        Objects.requireNonNull(
                costScore,
                "costScore must not be null"
        );

        Objects.requireNonNull(
                openingConfidenceScore,
                "openingConfidenceScore must not be null"
        );

        Objects.requireNonNull(
                timeEfficiencyScore,
                "timeEfficiencyScore must not be null"
        );

        Objects.requireNonNull(
                totalScore,
                "totalScore must not be null"
        );
    }
}