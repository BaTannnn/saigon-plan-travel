package com.saigonplantravel.backend.itinerary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateItineraryPreviewRequest(@NotBlank @Size(min = 3, max = 1000) String preferenceDescription) {}
