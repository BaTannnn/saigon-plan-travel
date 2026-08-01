package com.saigonplantravel.backend.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TripResponse(
        UUID publicId,
        LocalDate tripDate,

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        BigDecimal budget,
        StartLocationResponse startLocation,
        TravelPace travelPace,
        EnvironmentPreference environmentPreference,
        List<CategoryResponse> categoryPreferences,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public TripResponse {
        categoryPreferences =
                List.copyOf(categoryPreferences);
    }
}