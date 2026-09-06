package com.saigonplantravel.backend.itinerary.model;

import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import java.util.Map;

public record GeneratedItineraryPreview(ItineraryPlan plan, Map<String, String> reasonsByPlaceSlug) {

    public GeneratedItineraryPreview {
        reasonsByPlaceSlug = Map.copyOf(reasonsByPlaceSlug);
    }
}
