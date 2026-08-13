package com.saigonplantravel.backend.trip.mapper;

import com.saigonplantravel.backend.trip.dto.StartLocationResponse;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public TripResponse toResponse(Trip trip) {
        StartLocationResponse startLocation = new StartLocationResponse(
                trip.getStartLocationLabel(), trip.getStartLatitude(), trip.getStartLongitude());

        return new TripResponse(
                trip.getPublicId(),
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getBudget(),
                startLocation,
                trip.getTravelPace(),
                trip.getEnvironmentPreference(),
                trip.getCreatedAt(),
                trip.getUpdatedAt());
    }

    public TripSummaryResponse toSummaryResponse(Trip trip) {
        return new TripSummaryResponse(
                trip.getPublicId(),
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getBudget(),
                trip.getStartLocationLabel(),
                trip.getTravelPace(),
                trip.getEnvironmentPreference(),
                trip.getUpdatedAt());
    }
}
