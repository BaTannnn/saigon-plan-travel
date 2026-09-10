package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiAssistantResponse(
        String answer, @JsonProperty("suggested_places") List<SuggestedPlace> suggestedPlaces, List<Source> sources) {

    public record SuggestedPlace(String slug, String reason) {}

    public record Source(
            String type,
            @JsonProperty("place_slug") String placeSlug,
            @JsonProperty("place_name") String placeName,
            String section,
            String title,
            @JsonProperty("page_number") Integer pageNumber,
            @JsonProperty("source_label") String sourceLabel) {}
}
