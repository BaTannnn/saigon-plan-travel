package com.saigonplantravel.backend.itinerary.dto;

import java.math.BigDecimal;

public record ItineraryPlaceResponse(
        String slug,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String primaryImageUrl,
        BigDecimal minCost,
        BigDecimal maxCost) {

    public ItineraryPlaceResponse(String slug, String name, BigDecimal latitude, BigDecimal longitude) {
        this(slug, name, latitude, longitude, null, null, null);
    }

    public ItineraryPlaceResponse(
            String slug, String name, BigDecimal latitude, BigDecimal longitude, String primaryImageUrl) {
        this(slug, name, latitude, longitude, primaryImageUrl, null, null);
    }
}
