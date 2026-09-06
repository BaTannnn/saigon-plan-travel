package com.saigonplantravel.backend.itinerary.dto;

import java.util.List;

public record ItineraryGenerationPreviewResponse(
        List<GeneratedItineraryStopResponse> stops,
        ItinerarySummaryResponse summary,
        List<ItineraryIssueResponse> issues) {

    public ItineraryGenerationPreviewResponse {
        stops = List.copyOf(stops);
        issues = List.copyOf(issues);
    }
}
