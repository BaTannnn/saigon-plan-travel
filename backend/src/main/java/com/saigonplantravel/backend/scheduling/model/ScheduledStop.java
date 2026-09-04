package com.saigonplantravel.backend.scheduling.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduledStop(
        SchedulingPlace place,
        LocalDateTime arrivalDateTime,
        LocalDateTime visitStartDateTime,
        LocalDateTime visitEndDateTime,
        int travelMinutes,
        double travelDistanceKm,
        BigDecimal estimatedCost,
        boolean withinOpeningWindow,
        boolean withinTripWindow) {

    public LocalTime arrivalTime() {
        return arrivalDateTime.toLocalTime();
    }

    public LocalTime visitStartTime() {
        return visitStartDateTime.toLocalTime();
    }

    public LocalTime visitEndTime() {
        return visitEndDateTime.toLocalTime();
    }

    public boolean temporallyFeasible() {
        return withinOpeningWindow && withinTripWindow;
    }
}
