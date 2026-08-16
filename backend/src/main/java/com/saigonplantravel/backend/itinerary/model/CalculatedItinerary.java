package com.saigonplantravel.backend.itinerary.model;

import com.saigonplantravel.backend.itinerary.validation.ItineraryIssue;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import java.util.List;

public record CalculatedItinerary(ItineraryPlan plan, List<ItineraryIssue> issues) {

    public CalculatedItinerary {
        issues = List.copyOf(issues);
    }
}
