package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiAssistantRequest(
        String message,
        List<ConversationMessage> history,
        @JsonProperty("excluded_place_slugs") List<String> excludedPlaceSlugs) {

    public record ConversationMessage(String role, String content) {}
}
