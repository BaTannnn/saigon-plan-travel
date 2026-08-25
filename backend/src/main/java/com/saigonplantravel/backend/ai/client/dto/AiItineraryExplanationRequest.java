package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiItineraryExplanationRequest(String preference, @JsonProperty("place_slugs") List<String> placeSlugs) {

    public AiItineraryExplanationRequest {
        placeSlugs = List.copyOf(placeSlugs);
    }
}
