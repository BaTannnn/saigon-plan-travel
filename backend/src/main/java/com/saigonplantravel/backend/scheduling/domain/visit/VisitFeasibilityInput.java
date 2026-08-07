package com.saigonplantravel.backend.scheduling.domain.visit;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;

import java.time.LocalDateTime;
import java.util.Objects;

public record VisitFeasibilityInput(
        LocalDateTime departureAt,
        LocalDateTime tripEndAt,
        int travelMinutes,
        int visitMinutes,
        OpeningHoursSnapshot openingHours
) {

    public VisitFeasibilityInput {
        Objects.requireNonNull(
                departureAt,
                "departureAt must not be null"
        );

        Objects.requireNonNull(
                tripEndAt,
                "tripEndAt must not be null"
        );

        Objects.requireNonNull(
                openingHours,
                "openingHours must not be null"
        );

        if (!departureAt.isBefore(tripEndAt)) {
            throw new IllegalArgumentException(
                    "departureAt must be before tripEndAt"
            );
        }

        if (travelMinutes < 0) {
            throw new IllegalArgumentException(
                    "travelMinutes must not be negative"
            );
        }

        if (visitMinutes <= 0) {
            throw new IllegalArgumentException(
                    "visitMinutes must be positive"
            );
        }
    }
}