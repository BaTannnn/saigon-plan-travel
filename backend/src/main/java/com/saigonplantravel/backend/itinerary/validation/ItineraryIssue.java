package com.saigonplantravel.backend.itinerary.validation;

public record ItineraryIssue(ItineraryIssueType type, String placeSlug) {

    public static ItineraryIssue forPlace(ItineraryIssueType type, String placeSlug) {

        return new ItineraryIssue(type, placeSlug);
    }

    public static ItineraryIssue forPlan(ItineraryIssueType type) {

        return new ItineraryIssue(type, null);
    }
}
