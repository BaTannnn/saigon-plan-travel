package com.saigonplantravel.backend.itinerary.dto;

import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ItineraryItemResponse(
        UUID publicId,
        Integer sequenceNo,
        PlaceSummaryResponse place,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
