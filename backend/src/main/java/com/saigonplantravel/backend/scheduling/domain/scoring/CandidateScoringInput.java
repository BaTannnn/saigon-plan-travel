package com.saigonplantravel.backend.scheduling.domain.scoring;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;

import java.math.BigDecimal;
import java.util.Objects;

public record CandidateScoringInput(
        int matchedPreferredCategoryCount,
        int totalPreferredCategoryCount,
        BigDecimal distanceKilometers,
        BigDecimal minCost,
        BigDecimal remainingBudget,
        OpeningHoursStatus openingHoursStatus,
        int travelMinutes,
        int waitingMinutes,
        int adjustedVisitMinutes
) {

    public CandidateScoringInput {
        if (matchedPreferredCategoryCount <= 0) {
            throw new IllegalArgumentException(
                    "matchedPreferredCategoryCount must be positive"
            );
        }

        if (totalPreferredCategoryCount <= 0) {
            throw new IllegalArgumentException(
                    "totalPreferredCategoryCount must be positive"
            );
        }

        if (matchedPreferredCategoryCount
                > totalPreferredCategoryCount) {
            throw new IllegalArgumentException(
                    "matchedPreferredCategoryCount must not exceed totalPreferredCategoryCount"
            );
        }

        Objects.requireNonNull(
                distanceKilometers,
                "distanceKilometers must not be null"
        );

        Objects.requireNonNull(
                minCost,
                "minCost must not be null"
        );

        Objects.requireNonNull(
                remainingBudget,
                "remainingBudget must not be null"
        );

        Objects.requireNonNull(
                openingHoursStatus,
                "openingHoursStatus must not be null"
        );

        if (distanceKilometers.signum() < 0) {
            throw new IllegalArgumentException(
                    "distanceKilometers must not be negative"
            );
        }

        if (minCost.signum() < 0) {
            throw new IllegalArgumentException(
                    "minCost must not be negative"
            );
        }

        if (remainingBudget.signum() < 0) {
            throw new IllegalArgumentException(
                    "remainingBudget must not be negative"
            );
        }

        if (openingHoursStatus == OpeningHoursStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "closed place must not be scored"
            );
        }

        if (travelMinutes < 0) {
            throw new IllegalArgumentException(
                    "travelMinutes must not be negative"
            );
        }

        if (waitingMinutes < 0) {
            throw new IllegalArgumentException(
                    "waitingMinutes must not be negative"
            );
        }

        if (adjustedVisitMinutes <= 0) {
            throw new IllegalArgumentException(
                    "adjustedVisitMinutes must be positive"
            );
        }
    }
}