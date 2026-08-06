package com.saigonplantravel.backend.scheduling.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class TravelTimeEstimator {

    private static final BigDecimal MINUTES_PER_HOUR =
            BigDecimal.valueOf(60);

    private final DistanceCalculator distanceCalculator;
    private final SchedulingPolicy schedulingPolicy;

    public TravelTimeEstimator(
            DistanceCalculator distanceCalculator,
            SchedulingPolicy schedulingPolicy
    ) {
        this.distanceCalculator =
                Objects.requireNonNull(
                        distanceCalculator,
                        "distanceCalculator must not be null"
                );

        this.schedulingPolicy =
                Objects.requireNonNull(
                        schedulingPolicy,
                        "schedulingPolicy must not be null"
                );
    }

    public TravelEstimate estimate(
            GeoPoint from,
            GeoPoint to
    ) {
        Objects.requireNonNull(
                from,
                "from must not be null"
        );

        Objects.requireNonNull(
                to,
                "to must not be null"
        );

        BigDecimal distanceKilometers =
                distanceCalculator.calculateKilometers(
                        from,
                        to
                );

        validateDistance(
                distanceKilometers
        );

        int travelMinutes =
                calculateTravelMinutes(
                        distanceKilometers
                );

        return new TravelEstimate(
                distanceKilometers,
                travelMinutes
        );
    }

    private int calculateTravelMinutes(
            BigDecimal distanceKilometers
    ) {
        BigDecimal movementMinutes =
                distanceKilometers
                        .multiply(
                                MINUTES_PER_HOUR
                        )
                        .divide(
                                schedulingPolicy
                                        .averageSpeedKmh(),
                                0,
                                RoundingMode.CEILING
                        );

        return Math.addExact(
                movementMinutes.intValueExact(),
                schedulingPolicy
                        .fixedTransferMinutes()
        );
    }

    private static void validateDistance(
            BigDecimal distanceKilometers
    ) {
        if (distanceKilometers == null) {
            throw new IllegalStateException(
                    "DistanceCalculator returned null"
            );
        }

        if (distanceKilometers.signum() < 0) {
            throw new IllegalStateException(
                    "DistanceCalculator returned a negative distance"
            );
        }
    }
}