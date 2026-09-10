package com.saigonplantravel.backend.assistant.dto;

import java.util.List;

public record AssistantMessageResponse(String answer, List<SuggestedPlace> suggestedPlaces, List<Source> sources) {

    public record SuggestedPlace(Long placeId, String slug, String name, String primaryImageUrl, String reason) {}

    public sealed interface Source permits PlaceSource, DocumentSource {
        String type();
    }

    public record PlaceSource(String type, String placeSlug, String placeName, String section) implements Source {
        public PlaceSource(String placeSlug, String placeName, String section) {
            this("PLACE", placeSlug, placeName, section);
        }
    }

    public record DocumentSource(String type, String title, Integer pageNumber, String sourceLabel) implements Source {
        public DocumentSource(String title, Integer pageNumber, String sourceLabel) {
            this("DOCUMENT", title, pageNumber, sourceLabel);
        }
    }
}
