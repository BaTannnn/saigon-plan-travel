package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.scheduling.domain.travel.GeoPoint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class SchedulingState {

    private GeoPoint currentLocation;

    private LocalDateTime currentTime;

    private BigDecimal remainingBudget;

    private int sequence;

    private final List<PlaceSchedulingCandidate>
            unscheduledCandidates;

    private final List<EvaluatedCandidate>
            scheduledCandidates;

    private SchedulingState(
            GeoPoint currentLocation,
            LocalDateTime currentTime,
            BigDecimal remainingBudget,
            List<PlaceSchedulingCandidate> unscheduledCandidates
    ) {
        this.currentLocation =
                Objects.requireNonNull(
                        currentLocation,
                        "currentLocation must not be null"
                );

        this.currentTime =
                Objects.requireNonNull(
                        currentTime,
                        "currentTime must not be null"
                );

        this.remainingBudget =
                Objects.requireNonNull(
                        remainingBudget,
                        "remainingBudget must not be null"
                );

        this.unscheduledCandidates =
                new ArrayList<>(
                        Objects.requireNonNull(
                                unscheduledCandidates,
                                "unscheduledCandidates must not be null"
                        )
                );

        this.scheduledCandidates =
                new ArrayList<>();

        this.sequence = 0;
    }

    static SchedulingState initialize(
            SchedulingInput input
    ) {
        Objects.requireNonNull(
                input,
                "input must not be null"
        );

        GeoPoint origin =
                new GeoPoint(
                        input.trip()
                                .startLatitude(),
                        input.trip()
                                .startLongitude()
                );

        LocalDateTime startAt =
                LocalDateTime.of(
                        input.trip()
                                .tripDate(),
                        input.trip()
                                .startTime()
                );

        return new SchedulingState(
                origin,
                startAt,
                input.trip().budget(),
                input.candidates()
        );
    }
    void applySelection(
            EvaluatedCandidate selected
    ) {
        Objects.requireNonNull(
                selected,
                "selected must not be null"
        );

        PlaceSchedulingCandidate candidate =
                selected.candidate();

        boolean existsInUnscheduled =
                unscheduledCandidates
                        .stream()
                        .anyMatch(
                                unscheduled ->
                                        unscheduled
                                                .placeId()
                                                .equals(
                                                        candidate.placeId()
                                                )
                        );

        if (!existsInUnscheduled) {
            throw new IllegalStateException(
                    "selected candidate is not unscheduled: placeId="
                            + candidate.placeId()
            );
        }

        if (!selected
                .visitSchedule()
                .departureAt()
                .equals(currentTime)) {
            throw new IllegalStateException(
                    "selected candidate was evaluated from a stale scheduling state"
            );
        }

        if (candidate
                .minCost()
                .compareTo(
                        remainingBudget
                ) > 0) {
            throw new IllegalStateException(
                    "selected candidate exceeds remaining budget"
            );
        }

        currentLocation =
                new GeoPoint(
                        candidate.latitude(),
                        candidate.longitude()
                );

        currentTime =
                selected
                        .visitSchedule()
                        .visitEndAt();

        remainingBudget =
                remainingBudget.subtract(
                        candidate.minCost()
                );

        sequence =
                Math.incrementExact(
                        sequence
                );

        unscheduledCandidates.removeIf(
                unscheduled ->
                        unscheduled
                                .placeId()
                                .equals(
                                        candidate.placeId()
                                )
        );

        scheduledCandidates.add(
                selected
        );
    }
    GeoPoint currentLocation() {
        return currentLocation;
    }

    LocalDateTime currentTime() {
        return currentTime;
    }

    BigDecimal remainingBudget() {
        return remainingBudget;
    }

    int sequence() {
        return sequence;
    }

    List<PlaceSchedulingCandidate>
    unscheduledCandidates() {
        return List.copyOf(
                unscheduledCandidates
        );
    }

    List<EvaluatedCandidate>
    scheduledCandidates() {
        return List.copyOf(
                scheduledCandidates
        );
    }
}