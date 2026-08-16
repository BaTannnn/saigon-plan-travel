package com.saigonplantravel.backend.itinerary.dto;

import java.util.List;
import java.util.UUID;

public record ItineraryDetailResponse(
        UUID publicId,
        UUID tripPublicId,
        List<ItineraryDetailItemResponse> items,
        ItinerarySummaryResponse summary,
        List<ItineraryIssueResponse> issues) {}
