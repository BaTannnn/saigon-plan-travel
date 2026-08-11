package com.saigonplantravel.backend.trip.mapper;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.trip.dto.StartLocationResponse;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public TripResponse toResponse(Trip trip, List<CategoryResponse> categoryPreferences) {
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
                categoryPreferences,
                trip.getCreatedAt(),
                trip.getUpdatedAt());
    }

    public TripSummaryResponse toSummaryResponse(Trip trip, List<CategoryResponse> categoryPreferences) {
        return new TripSummaryResponse(
                trip.getPublicId(),
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getBudget(),
                trip.getStartLocationLabel(),
                trip.getTravelPace(),
                trip.getEnvironmentPreference(),
                categoryPreferences,
                trip.getUpdatedAt());
    }
}
