package com.saigonplantravel.backend.trip.service;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record TripSchedulingSnapshot(
        Long tripId,
        UUID publicId,
        LocalDate tripDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal budget,
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        TravelPace travelPace,
        EnvironmentPreference environmentPreference,
        Set<Long> preferredCategoryIds,
        OffsetDateTime updatedAt
) {

    public TripSchedulingSnapshot {
        preferredCategoryIds =
                Set.copyOf(preferredCategoryIds);
    }
}