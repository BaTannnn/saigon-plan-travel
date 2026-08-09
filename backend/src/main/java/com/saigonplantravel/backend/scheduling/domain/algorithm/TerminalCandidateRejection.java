package com.saigonplantravel.backend.scheduling.domain.algorithm;

import java.util.Objects;

public record TerminalCandidateRejection(
        Long placeId,
        CandidateRejectionReason reason
) {

    public TerminalCandidateRejection {
        Objects.requireNonNull(
                placeId,
                "placeId must not be null"
        );

        Objects.requireNonNull(
                reason,
                "reason must not be null"
        );

        if (placeId <= 0) {
            throw new IllegalArgumentException(
                    "placeId must be positive"
            );
        }
    }
}