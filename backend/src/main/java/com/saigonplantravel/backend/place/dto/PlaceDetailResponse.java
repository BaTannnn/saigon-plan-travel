package com.saigonplantravel.backend.place.dto;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;

import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String fullDescription,
        String address,
        String administrativeUnitName,
        AdministrativeUnitType administrativeUnitType,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer estimatedVisitMinutes,
        BigDecimal minCost,
        BigDecimal maxCost,
        boolean indoor,
        List<CategoryResponse> categories,
        List<OpeningHourResponse> openingHours
) {
}
