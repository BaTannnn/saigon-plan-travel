package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPlaceCandidateResponse(
        @JsonProperty("place_slug") String placeSlug,
        @JsonProperty("place_name") String placeName,
        @JsonProperty("matched_section") String matchedSection,
        @JsonProperty("semantic_score") double semanticScore) {}
