package com.saigonplantravel.backend.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripSummaryResponse(
        UUID publicId,
        LocalDate tripDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        BigDecimal budget,
        String startLocationLabel,
        TravelPace travelPace,
        EnvironmentPreference environmentPreference,
        OffsetDateTime updatedAt) {}
