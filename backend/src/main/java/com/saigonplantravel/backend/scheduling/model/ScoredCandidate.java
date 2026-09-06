package com.saigonplantravel.backend.scheduling.model;

public record ScoredCandidate(
        SchedulingPlace place,
        String matchedSection,
        double semanticScore,
        double travelDistanceKm,
        int travelMinutes,
        double travelScore,
        double budgetScore,
        double environmentScore,
        double finalScore) {}
