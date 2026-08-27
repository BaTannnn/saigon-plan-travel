package com.saigonplantravel.backend.scheduling.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ScheduledStop(
        SchedulingPlace place,
        LocalTime arrivalTime,
        LocalTime visitStartTime,
        LocalTime visitEndTime,
        int travelMinutes,
        double travelDistanceKm,
        BigDecimal estimatedCost,
        boolean withinOpeningWindow,
        boolean withinTripWindow) {

    public boolean temporallyFeasible() {
        return withinOpeningWindow && withinTripWindow;
    }
}
