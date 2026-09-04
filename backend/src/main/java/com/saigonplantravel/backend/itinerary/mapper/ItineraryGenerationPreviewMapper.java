package com.saigonplantravel.backend.itinerary.mapper;

import com.saigonplantravel.backend.itinerary.dto.GeneratedItineraryStopResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryGenerationPreviewResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryPlaceResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryScheduleResponse;
import com.saigonplantravel.backend.itinerary.dto.ItinerarySummaryResponse;
import com.saigonplantravel.backend.itinerary.model.GeneratedItineraryPreview;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class ItineraryGenerationPreviewMapper {

    public ItineraryGenerationPreviewResponse toResponse(GeneratedItineraryPreview preview) {
        var plan = preview.plan();

        List<GeneratedItineraryStopResponse> stops = IntStream.range(
                        0, plan.stops().size())
                .mapToObj(index -> {
                    var stop = plan.stops().get(index);

                    var place = stop.place();

                    return new GeneratedItineraryStopResponse(
                            index + 1,
                            new ItineraryPlaceResponse(
                                    place.slug(),
                                    place.name(),
                                    place.latitude(),
                                    place.longitude(),
                                    place.primaryImageUrl(),
                                    place.estimatedCost(),
                                    place.maxCost()),
                            new ItineraryScheduleResponse(
                                    stop.arrivalTime(),
                                    stop.visitStartTime(),
                                    stop.visitEndTime(),
                                    stop.travelMinutes(),
                                    stop.travelDistanceKm(),
                                    stop.estimatedCost()),
                            preview.reasonsByPlaceSlug().get(place.slug()));
                })
                .toList();

        ItinerarySummaryResponse summary = new ItinerarySummaryResponse(
                plan.totalEstimatedCost(), plan.totalTravelMinutes(), plan.totalVisitMinutes(), plan.totalDistanceKm());

        return new ItineraryGenerationPreviewResponse(stops, summary, List.of());
    }
}
