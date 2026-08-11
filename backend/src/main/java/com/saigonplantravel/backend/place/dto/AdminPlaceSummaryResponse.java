package com.saigonplantravel.backend.place.dto;

import java.math.BigDecimal;

public record AdminPlaceSummaryResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer estimatedVisitMinutes,
        BigDecimal minCost,
        BigDecimal maxCost,
        Boolean indoor,
        Boolean active) {}
