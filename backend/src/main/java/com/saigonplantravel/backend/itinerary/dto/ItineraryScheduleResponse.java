package com.saigonplantravel.backend.itinerary.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ItineraryScheduleResponse(
        LocalTime arrivalTime,
        LocalTime visitStartTime,
        LocalTime visitEndTime,
        int travelMinutes,
        double travelDistanceKm,
        BigDecimal estimatedCost) {}
