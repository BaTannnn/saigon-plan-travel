package com.saigonplantravel.backend.scheduling.model;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import java.math.BigDecimal;
import java.time.LocalTime;

public record PlanningContext(
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal budget,
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        EnvironmentPreference environmentPreference) {}
