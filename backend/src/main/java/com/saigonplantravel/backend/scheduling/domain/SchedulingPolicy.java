package com.saigonplantravel.backend.scheduling.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record SchedulingPolicy(
        SchedulingAlgorithmVersion algorithmVersion,
        BigDecimal averageSpeedKmh,
        int fixedTransferMinutes,
        int maxCandidates
) {

    private static final int MIN_CANDIDATES =
            1;

    private static final int MAX_CANDIDATES =
            100;

    public SchedulingPolicy {
        Objects.requireNonNull(
                algorithmVersion,
                "algorithmVersion must not be null"
        );

        Objects.requireNonNull(
                averageSpeedKmh,
                "averageSpeedKmh must not be null"
        );

        if (averageSpeedKmh.signum() <= 0) {
            throw new IllegalArgumentException(
                    "averageSpeedKmh must be positive"
            );
        }

        if (fixedTransferMinutes < 0) {
            throw new IllegalArgumentException(
                    "fixedTransferMinutes must not be negative"
            );
        }

        if (maxCandidates < MIN_CANDIDATES
                || maxCandidates > MAX_CANDIDATES) {
            throw new IllegalArgumentException(
                    "maxCandidates must be between 1 and 100"
            );
        }
    }
}