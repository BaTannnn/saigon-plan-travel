package com.saigonplantravel.backend.itinerary.mapper;

import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailItemResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryIssueResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryPlaceResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryScheduleResponse;
import com.saigonplantravel.backend.itinerary.dto.ItinerarySummaryResponse;
import com.saigonplantravel.backend.itinerary.entity.Itinerary;
import com.saigonplantravel.backend.itinerary.entity.ItineraryItem;
import com.saigonplantravel.backend.itinerary.model.CalculatedItinerary;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class ItineraryDetailMapper {

    public ItineraryDetailResponse toResponse(
            UUID tripPublicId, Itinerary itinerary, CalculatedItinerary calculatedItinerary) {

        ItineraryPlan plan = calculatedItinerary.plan();

        List<ItineraryItem> items = itinerary.getItems();

        List<ScheduledStop> stops = plan.stops();

        if (items.size() != stops.size()) {
            throw new IllegalStateException("Itinerary items and calculated stops must have the same size");
        }

        List<ItineraryDetailItemResponse> itemResponses = IntStream.range(0, items.size())
                .mapToObj(index -> toItemResponse(items.get(index), stops.get(index)))
                .toList();

        ItinerarySummaryResponse summary = new ItinerarySummaryResponse(
                plan.totalEstimatedCost(), plan.totalTravelMinutes(), plan.totalVisitMinutes(), plan.totalDistanceKm());

        List<ItineraryIssueResponse> issues = calculatedItinerary.issues().stream()
                .map(issue -> new ItineraryIssueResponse(issue.type(), issue.placeSlug()))
                .toList();

        return new ItineraryDetailResponse(itinerary.getPublicId(), tripPublicId, itemResponses, summary, issues);
    }

    private ItineraryDetailItemResponse toItemResponse(ItineraryItem item, ScheduledStop stop) {

        ItineraryPlaceResponse place = new ItineraryPlaceResponse(
                item.getPlace().getSlug(),
                item.getPlace().getName(),
                item.getPlace().getLatitude(),
                item.getPlace().getLongitude(),
                item.getPlace().getPrimaryImageUrl(),
                item.getPlace().getMinCost(),
                item.getPlace().getMaxCost());

        ItineraryScheduleResponse schedule = new ItineraryScheduleResponse(
                stop.arrivalTime(),
                stop.visitStartTime(),
                stop.visitEndTime(),
                stop.travelMinutes(),
                stop.travelDistanceKm(),
                stop.estimatedCost());

        return new ItineraryDetailItemResponse(item.getPublicId(), item.getSequenceNo(), place, schedule);
    }

    public ItineraryDetailResponse toEmptyResponse(UUID tripPublicId, CalculatedItinerary calculatedItinerary) {

        ItineraryPlan plan = calculatedItinerary.plan();

        ItinerarySummaryResponse summary = new ItinerarySummaryResponse(
                plan.totalEstimatedCost(), plan.totalTravelMinutes(), plan.totalVisitMinutes(), plan.totalDistanceKm());

        List<ItineraryIssueResponse> issues = calculatedItinerary.issues().stream()
                .map(issue -> new ItineraryIssueResponse(issue.type(), issue.placeSlug()))
                .toList();

        return new ItineraryDetailResponse(null, tripPublicId, List.of(), summary, issues);
    }
}
