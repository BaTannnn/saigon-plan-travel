package com.saigonplantravel.backend.scheduling.model;

import java.math.BigDecimal;
import java.util.List;

public record ItineraryPlan(
        List<ScheduledStop> stops,
        BigDecimal totalEstimatedCost,
        int totalTravelMinutes,
        int totalVisitMinutes,
        double totalDistanceKm) {

    public ItineraryPlan {
        stops = List.copyOf(stops);
    }
}
