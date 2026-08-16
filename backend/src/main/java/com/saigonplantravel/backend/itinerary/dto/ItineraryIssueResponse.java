package com.saigonplantravel.backend.itinerary.dto;

import com.saigonplantravel.backend.itinerary.validation.ItineraryIssueType;

public record ItineraryIssueResponse(ItineraryIssueType type, String placeSlug) {}
