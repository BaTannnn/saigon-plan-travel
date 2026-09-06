package com.saigonplantravel.backend.place.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String fullDescription,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer estimatedVisitMinutes,
        BigDecimal minCost,
        BigDecimal maxCost,
        boolean indoor,
        List<CategoryResponse> categories,
        List<OpeningHourResponse> openingHours,
        String primaryImageUrl) {

    public PlaceDetailResponse(
            Long id,
            String name,
            String slug,
            String shortDescription,
            String fullDescription,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer estimatedVisitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            boolean indoor,
            List<CategoryResponse> categories,
            List<OpeningHourResponse> openingHours) {
        this(
                id,
                name,
                slug,
                shortDescription,
                fullDescription,
                address,
                latitude,
                longitude,
                estimatedVisitMinutes,
                minCost,
                maxCost,
                indoor,
                categories,
                openingHours,
                null);
    }
}
