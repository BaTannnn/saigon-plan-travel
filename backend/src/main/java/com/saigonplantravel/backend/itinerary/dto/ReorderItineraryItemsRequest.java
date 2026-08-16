package com.saigonplantravel.backend.itinerary.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderItineraryItemsRequest(@NotEmpty List<@NotNull UUID> itemPublicIds) {}
