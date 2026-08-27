package com.saigonplantravel.backend.scheduling.model;

import java.math.BigDecimal;

public record SchedulingPlace(
        String slug,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        int estimatedVisitMinutes,
        BigDecimal estimatedCost,
        boolean indoor,
        String primaryImageUrl,
        OpeningWindow openingWindow) {}
