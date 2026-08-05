package com.saigonplantravel.backend.place.scheduling;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;

import java.time.LocalTime;
import java.util.Objects;

public record OpeningHoursSnapshot(
        OpeningHoursStatus status,
        LocalTime openTime,
        LocalTime closeTime
) {

    public OpeningHoursSnapshot {
        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        switch (status) {
            case KNOWN_OPEN -> validateKnownOpen(
                    openTime,
                    closeTime
            );

            case CLOSED, UNKNOWN -> validateWithoutInterval(
                    status,
                    openTime,
                    closeTime
            );
        }
    }

    public static OpeningHoursSnapshot knownOpen(
            LocalTime openTime,
            LocalTime closeTime
    ) {
        return new OpeningHoursSnapshot(
                OpeningHoursStatus.KNOWN_OPEN,
                openTime,
                closeTime
        );
    }

    public static OpeningHoursSnapshot closed() {
        return new OpeningHoursSnapshot(
                OpeningHoursStatus.CLOSED,
                null,
                null
        );
    }

    public static OpeningHoursSnapshot unknown() {
        return new OpeningHoursSnapshot(
                OpeningHoursStatus.UNKNOWN,
                null,
                null
        );
    }

    private static void validateKnownOpen(
            LocalTime openTime,
            LocalTime closeTime
    ) {
        Objects.requireNonNull(
                openTime,
                "openTime must not be null for KNOWN_OPEN"
        );

        Objects.requireNonNull(
                closeTime,
                "closeTime must not be null for KNOWN_OPEN"
        );

        if (!openTime.isBefore(closeTime)) {
            throw new IllegalArgumentException(
                    "openTime must be before closeTime"
            );
        }
    }

    private static void validateWithoutInterval(
            OpeningHoursStatus status,
            LocalTime openTime,
            LocalTime closeTime
    ) {
        if (openTime != null || closeTime != null) {
            throw new IllegalArgumentException(
                    status
                            + " must not contain an opening interval"
            );
        }
    }
}