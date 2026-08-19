package com.saigonplantravel.backend.place.dto.admin;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import java.math.BigDecimal;
import java.util.List;

public record AdminPlaceDetailResponse(
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
        Boolean indoor,
        List<CategoryResponse> categories,
        List<OpeningHourResponse> openingHours,
        Boolean active,
        String primaryImageUrl) {

    public AdminPlaceDetailResponse(
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
            Boolean indoor,
            List<CategoryResponse> categories,
            List<OpeningHourResponse> openingHours,
            Boolean active) {
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
                active,
                null);
    }
}
