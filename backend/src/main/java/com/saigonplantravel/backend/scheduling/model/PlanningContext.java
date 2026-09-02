package com.saigonplantravel.backend.scheduling.model;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record PlanningContext(
        LocalDate tripDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal budget,
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        EnvironmentPreference environmentPreference) {

    public LocalDateTime startDateTime() {
        return LocalDateTime.of(tripDate, startTime);
    }

    public LocalDateTime endDateTime() {
        return LocalDateTime.of(tripDate, endTime);
    }
}
