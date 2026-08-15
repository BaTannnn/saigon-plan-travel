package com.saigonplantravel.backend.recommendation.model;

import com.saigonplantravel.backend.place.entity.Place;

public record ScoredCandidate(
        Place place,
        String matchedSection,
        double semanticScore,
        double distanceKm,
        double distanceScore,
        double budgetScore,
        double environmentScore,
        double finalScore) {
}