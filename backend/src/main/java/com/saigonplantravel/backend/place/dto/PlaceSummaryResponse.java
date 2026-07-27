package com.saigonplantravel.backend.place.dto;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;

import java.math.BigDecimal;

public record PlaceSummaryResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String administrativeUnitName,
        AdministrativeUnitType administrativeUnitType,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer estimatedVisitMinutes,
        BigDecimal minCost,
        BigDecimal maxCost,
        Boolean indoor
) {
}
