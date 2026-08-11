package com.saigonplantravel.backend.place.dto;

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
        Boolean active) {}
