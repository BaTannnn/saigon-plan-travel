package com.saigonplantravel.backend.scheduling.model;

import com.saigonplantravel.backend.place.entity.Place;
import java.math.BigDecimal;
import java.time.LocalTime;

public record ScheduledStop(
        Place place,
        LocalTime arrivalTime,
        LocalTime visitStartTime,
        LocalTime visitEndTime,
        int travelMinutes,
        double travelDistanceKm,
        BigDecimal estimatedCost) {}
