package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.trip.domain.TravelPace;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PaceDurationPolicy {

    private static final BigDecimal RELAXED_MULTIPLIER =
            new BigDecimal("1.25");

    private static final BigDecimal BALANCED_MULTIPLIER =
            new BigDecimal("1.00");

    private static final BigDecimal FAST_MULTIPLIER =
            new BigDecimal("0.80");

    private static final BigDecimal ROUNDING_INTERVAL_MINUTES =
            BigDecimal.valueOf(5);

    public int adjustVisitMinutes(
            int baseVisitMinutes,
            TravelPace travelPace
    ) {
        if (baseVisitMinutes <= 0) {
            throw new IllegalArgumentException(
                    "baseVisitMinutes must be positive"
            );
        }

        Objects.requireNonNull(
                travelPace,
                "travelPace must not be null"
        );

        BigDecimal rawVisitMinutes =
                BigDecimal
                        .valueOf(baseVisitMinutes)
                        .multiply(
                                multiplierFor(
                                        travelPace
                                )
                        );

        BigDecimal roundedIntervals =
                rawVisitMinutes.divide(
                        ROUNDING_INTERVAL_MINUTES,
                        0,
                        RoundingMode.CEILING
                );

        return roundedIntervals
                .multiply(
                        ROUNDING_INTERVAL_MINUTES
                )
                .intValueExact();
    }

    private static BigDecimal multiplierFor(
            TravelPace travelPace
    ) {
        return switch (travelPace) {
            case RELAXED ->
                    RELAXED_MULTIPLIER;

            case BALANCED ->
                    BALANCED_MULTIPLIER;

            case FAST ->
                    FAST_MULTIPLIER;
        };
    }
}