package com.saigonplantravel.backend.itinerary.dto;

import java.math.BigDecimal;

public record ItinerarySummaryResponse(
        BigDecimal totalEstimatedCost, int totalTravelMinutes, int totalVisitMinutes, double totalDistanceKm) {}
