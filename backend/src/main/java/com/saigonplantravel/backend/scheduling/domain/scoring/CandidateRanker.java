package com.saigonplantravel.backend.scheduling.domain.scoring;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CandidateRanker {

    private static final Comparator<EvaluatedCandidate>
            BEST_FIRST =
            Comparator
                    .comparing(
                            (EvaluatedCandidate evaluated) ->
                                    evaluated
                                            .score()
                                            .totalScore(),
                            Comparator.reverseOrder()
                    )
                    .thenComparingLong(
                            evaluated ->
                                    evaluated
                                            .visitSchedule()
                                            .waitingMinutes()
                    )
                    .thenComparing(
                            evaluated ->
                                    evaluated
                                            .travelEstimate()
                                            .distanceKilometers()
                    )
                    .thenComparing(
                            evaluated ->
                                    evaluated
                                            .candidate()
                                            .minCost()
                    )
                    .thenComparing(
                            evaluated ->
                                    evaluated
                                            .candidate()
                                            .name()
                    )
                    .thenComparing(
                            evaluated ->
                                    evaluated
                                            .candidate()
                                            .placeId()
                    );

    public Optional<EvaluatedCandidate> selectBest(
            List<EvaluatedCandidate> candidates
    ) {
        Objects.requireNonNull(
                candidates,
                "candidates must not be null"
        );

        List<EvaluatedCandidate> snapshot =
                List.copyOf(
                        candidates
                );

        return snapshot
                .stream()
                .min(
                        BEST_FIRST
                );
    }
}