package com.saigonplantravel.backend.itinerary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ApplyGeneratedItineraryRequest(@NotEmpty List<@NotBlank String> placeSlugs) {}
