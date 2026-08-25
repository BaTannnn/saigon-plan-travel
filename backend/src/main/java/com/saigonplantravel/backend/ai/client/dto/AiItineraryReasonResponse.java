package com.saigonplantravel.backend.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiItineraryReasonResponse(@JsonProperty("place_slug") String placeSlug, String reason) {}
