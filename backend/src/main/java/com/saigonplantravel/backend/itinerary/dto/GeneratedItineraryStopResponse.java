package com.saigonplantravel.backend.itinerary.dto;

public record GeneratedItineraryStopResponse(
        int sequenceNo, ItineraryPlaceResponse place, ItineraryScheduleResponse schedule, String reason) {}
