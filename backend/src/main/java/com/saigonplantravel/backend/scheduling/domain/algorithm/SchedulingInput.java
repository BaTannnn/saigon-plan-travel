package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.SchedulingPolicy;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record SchedulingInput(
        TripSchedulingSnapshot trip,
        List<PlaceSchedulingCandidate> candidates,
        SchedulingPolicy policy,
        OffsetDateTime generatedAt
) {

    public SchedulingInput {
        Objects.requireNonNull(
                trip,
                "trip must not be null"
        );

        Objects.requireNonNull(
                candidates,
                "candidates must not be null"
        );

        Objects.requireNonNull(
                policy,
                "policy must not be null"
        );

        Objects.requireNonNull(
                generatedAt,
                "generatedAt must not be null"
        );

        List<PlaceSchedulingCandidate> canonicalCandidates =
                candidates
                        .stream()
                        .map(candidate ->
                                Objects.requireNonNull(
                                        candidate,
                                        "candidate must not be null"
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        PlaceSchedulingCandidate::placeId
                                )
                        )
                        .toList();

        if (canonicalCandidates.size()
                > policy.maxCandidates()) {
            throw new IllegalArgumentException(
                    "candidate count must not exceed maxCandidates"
            );
        }

        candidates =
                List.copyOf(
                        canonicalCandidates
                );
    }
}