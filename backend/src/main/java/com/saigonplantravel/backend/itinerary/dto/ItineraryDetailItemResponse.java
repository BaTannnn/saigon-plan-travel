package com.saigonplantravel.backend.itinerary.dto;

import java.util.UUID;

public record ItineraryDetailItemResponse(
        UUID publicId, Integer sequenceNo, ItineraryPlaceResponse place, ItineraryScheduleResponse schedule) {}
