package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiPlaceRecommendationRequest(
        String query,
        @JsonProperty("top_k") int topK,
        @JsonProperty("eligible_place_slugs") List<String> eligiblePlaceSlugs) {

    public AiPlaceRecommendationRequest {
        eligiblePlaceSlugs = List.copyOf(eligiblePlaceSlugs);
    }
}
