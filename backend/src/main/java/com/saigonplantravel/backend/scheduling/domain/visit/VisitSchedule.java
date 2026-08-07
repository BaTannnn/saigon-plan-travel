package com.saigonplantravel.backend.scheduling.domain.visit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record VisitSchedule(
        LocalDateTime departureAt,
        LocalDateTime arrivalAt,
        LocalDateTime visitStartAt,
        LocalDateTime visitEndAt
) {

    public VisitSchedule {
        Objects.requireNonNull(
                departureAt,
                "departureAt must not be null"
        );

        Objects.requireNonNull(
                arrivalAt,
                "arrivalAt must not be null"
        );

        Objects.requireNonNull(
                visitStartAt,
                "visitStartAt must not be null"
        );

        Objects.requireNonNull(
                visitEndAt,
                "visitEndAt must not be null"
        );

        if (arrivalAt.isBefore(departureAt)) {
            throw new IllegalArgumentException(
                    "arrivalAt must not be before departureAt"
            );
        }

        if (visitStartAt.isBefore(arrivalAt)) {
            throw new IllegalArgumentException(
                    "visitStartAt must not be before arrivalAt"
            );
        }

        if (!visitStartAt.isBefore(visitEndAt)) {
            throw new IllegalArgumentException(
                    "visitStartAt must be before visitEndAt"
            );
        }
    }

    public long waitingMinutes() {
        return Duration
                .between(
                        arrivalAt,
                        visitStartAt
                )
                .toMinutes();
    }
}