package com.saigonplantravel.backend.scheduling.mapper;

import com.saigonplantravel.backend.scheduling.dto.ItineraryResponse;
import com.saigonplantravel.backend.scheduling.entity.Itinerary;
import com.saigonplantravel.backend.scheduling.entity.ItineraryItem;
import com.saigonplantravel.backend.scheduling.entity.ItineraryWarning;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItineraryMapper {

    public ItineraryResponse toResponse(
            Itinerary itinerary,
            boolean stale
    ) {
        List<ItineraryResponse.Item> items = itinerary.getItems().stream()
                .map(this::toItem)
                .toList();
        List<ItineraryResponse.Warning> warnings = itinerary.getWarnings().stream()
                .map(this::toWarning)
                .toList();

        return new ItineraryResponse(
                itinerary.getPublicId(),
                itinerary.getTripPublicIdSnapshot(),
                itinerary.getAlgorithmVersion().name(),
                itinerary.getGeneratedAt(),
                stale,
                new ItineraryResponse.SchedulingAssumptions(
                        "HAVERSINE",
                        itinerary.getAverageSpeedKmh(),
                        itinerary.getFixedTransferMinutes(),
                        "MIN_COST",
                        false
                ),
                new ItineraryResponse.TripWindow(
                        itinerary.getTripDate(),
                        itinerary.getWindowStart(),
                        itinerary.getWindowEnd()
                ),
                new ItineraryResponse.PreferencesSnapshot(
                        itinerary.getTravelPace(),
                        itinerary.getEnvironmentPreference(),
                        itinerary.getPreferredCategoryIds()
                ),
                new ItineraryResponse.Coordinate(
                        itinerary.getOriginLatitude(),
                        itinerary.getOriginLongitude()
                ),
                items,
                new ItineraryResponse.Summary(
                        itinerary.getCandidateCount(),
                        itinerary.getScheduledCount(),
                        itinerary.getTotalDistanceKm(),
                        itinerary.getTotalEstimatedCost(),
                        itinerary.getRemainingBudget(),
                        itinerary.getTotalTravelMinutes(),
                        itinerary.getTotalVisitMinutes(),
                        itinerary.getTotalWaitingMinutes(),
                        itinerary.getRemainingMinutes()
                ),
                warnings
        );
    }

    private ItineraryResponse.Item toItem(ItineraryItem item) {
        return new ItineraryResponse.Item(
                item.getSequenceNo(),
                new ItineraryResponse.PlaceSnapshot(
                        item.getPlaceId(),
                        item.getPlaceName(),
                        item.getPlaceSlug(),
                        item.getPlaceAddress(),
                        item.getPlaceAdministrativeUnitName(),
                        item.getPlaceAdministrativeUnitType(),
                        item.getLatitude(),
                        item.getLongitude(),
                        item.isIndoor()
                ),
                new ItineraryResponse.TravelSnapshot(
                        item.getTravelMinutesFromPrevious(),
                        item.getDistanceKmFromPrevious()
                ),
                item.getArrivalTime(),
                item.getWaitingMinutes(),
                item.getVisitStart(),
                item.getVisitEnd(),
                item.getBaseVisitMinutes(),
                item.getVisitMinutes(),
                item.getEstimatedCost(),
                item.getSelectionScore(),
                new ItineraryResponse.OpeningHours(
                        item.getOpeningHoursStatus(),
                        item.getOpeningTime(),
                        item.getClosingTime()
                )
        );
    }

    private ItineraryResponse.Warning toWarning(ItineraryWarning warning) {
        return new ItineraryResponse.Warning(
                warning.getCode(),
                warning.getMessage(),
                warning.getItemSequence()
        );
    }
}
