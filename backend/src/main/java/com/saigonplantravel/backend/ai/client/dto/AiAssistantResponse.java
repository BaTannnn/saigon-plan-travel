package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiAssistantResponse(
        String answer, @JsonProperty("suggested_places") List<SuggestedPlace> suggestedPlaces) {

    public record SuggestedPlace(String slug, String reason) {}
}
