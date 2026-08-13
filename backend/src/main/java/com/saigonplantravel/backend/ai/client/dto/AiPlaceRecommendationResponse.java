package com.saigonplantravel.backend.ai.client.dto;

import java.util.List;

public record AiPlaceRecommendationResponse(
        List<AiPlaceCandidateResponse> candidates) {

    public AiPlaceRecommendationResponse {
        candidates = candidates == null
                ? List.of()
                : List.copyOf(candidates);
    }
}