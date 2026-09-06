package com.saigonplantravel.backend.trip.dto;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record SaveTripRequest(
        @NotNull LocalDate tripDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Digits(integer = 9, fraction = 2) @DecimalMin("0.00") @DecimalMax("100000000.00") BigDecimal budget,
        @NotNull @Valid StartLocationRequest startLocation,
        @NotNull TravelPace travelPace,
        @NotNull EnvironmentPreference environmentPreference) {}
