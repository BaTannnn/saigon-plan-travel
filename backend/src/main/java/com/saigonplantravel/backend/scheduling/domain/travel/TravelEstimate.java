package com.saigonplantravel.backend.scheduling.domain.travel;

import java.math.BigDecimal;
import java.util.Objects;

public record TravelEstimate(
        BigDecimal distanceKilometers,
        int travelMinutes
) {

    public TravelEstimate {
        Objects.requireNonNull(
                distanceKilometers,
                "distanceKilometers must not be null"
        );

        if (distanceKilometers.signum() < 0) {
            throw new IllegalArgumentException(
                    "distanceKilometers must not be negative"
            );
        }

        if (travelMinutes < 0) {
            throw new IllegalArgumentException(
                    "travelMinutes must not be negative"
            );
        }
    }
}