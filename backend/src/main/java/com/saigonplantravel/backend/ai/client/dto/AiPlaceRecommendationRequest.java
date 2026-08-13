package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPlaceRecommendationRequest(
        String query,
        @JsonProperty("top_k")
        int topK) {}