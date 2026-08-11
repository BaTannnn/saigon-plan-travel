package com.saigonplantravel.backend.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaveItineraryItemRequest(@NotNull @Positive Long placeId) {}
