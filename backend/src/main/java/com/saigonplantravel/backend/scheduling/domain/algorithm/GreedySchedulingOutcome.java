package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record GreedySchedulingOutcome(
        int candidatePoolSize,
        List<EvaluatedCandidate> scheduledCandidates,
        List<TerminalCandidateRejection> terminalRejections,
        LocalDateTime finalTime,
        BigDecimal remainingBudget
) {

    public GreedySchedulingOutcome {
        if (candidatePoolSize < 0) {
            throw new IllegalArgumentException(
                    "candidatePoolSize must not be negative"
            );
        }

        Objects.requireNonNull(
                scheduledCandidates,
                "scheduledCandidates must not be null"
        );

        Objects.requireNonNull(
                terminalRejections,
                "terminalRejections must not be null"
        );

        Objects.requireNonNull(
                finalTime,
                "finalTime must not be null"
        );

        Objects.requireNonNull(
                remainingBudget,
                "remainingBudget must not be null"
        );

        if (remainingBudget.signum() < 0) {
            throw new IllegalArgumentException(
                    "remainingBudget must not be negative"
            );
        }

        scheduledCandidates =
                List.copyOf(
                        scheduledCandidates
                );

        terminalRejections =
                List.copyOf(
                        terminalRejections
                );
    }
}