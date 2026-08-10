package com.saigonplantravel.backend.itinerary.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ItineraryResponse(
        UUID publicId,
        UUID tripPublicId,
        List<ItineraryItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
