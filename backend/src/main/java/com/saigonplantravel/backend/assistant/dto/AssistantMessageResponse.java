package com.saigonplantravel.backend.assistant.dto;

import java.util.List;

public record AssistantMessageResponse(String answer, List<SuggestedPlace> suggestedPlaces) {

    public record SuggestedPlace(Long placeId, String slug, String name, String primaryImageUrl, String reason) {}
}
