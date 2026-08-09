package com.saigonplantravel.backend.scheduling.dto;

public record RejectionSummary(
        int candidatePoolSize,
        int rejectedByEnvironment,
        int rejectedByBudget,
        int rejectedByClosedHours,
        int rejectedByTripWindow,
        int rejectedByClosingTime
) {
}
