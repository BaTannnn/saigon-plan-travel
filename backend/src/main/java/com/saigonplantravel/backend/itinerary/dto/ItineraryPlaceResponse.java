package com.saigonplantravel.backend.itinerary.dto;

import java.math.BigDecimal;

public record ItineraryPlaceResponse(
        String slug,
        String name,
        BigDecimal latitude,
        BigDecimal longitude) {}
