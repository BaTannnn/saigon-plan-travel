package com.saigonplantravel.backend.place.dto;

import java.math.BigDecimal;

public record PlaceSummaryResponse(
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
        String primaryImageUrl) {

    public PlaceSummaryResponse(
            Long id,
            String name,
            String slug,
            String shortDescription,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer estimatedVisitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            Boolean indoor) {
        this(
                id,
                name,
                slug,
                shortDescription,
                latitude,
                longitude,
                estimatedVisitMinutes,
                minCost,
                maxCost,
                indoor,
                null);
    }
}
