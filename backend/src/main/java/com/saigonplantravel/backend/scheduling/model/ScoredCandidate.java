package com.saigonplantravel.backend.scheduling.model;

import com.saigonplantravel.backend.place.entity.Place;

public record ScoredCandidate(
        Place place,
        String matchedSection,
        double semanticScore,
        double travelDistanceKm,
        int travelMinutes,
        double travelScore,
        double budgetScore,
        double environmentScore,
        double finalScore) {}
