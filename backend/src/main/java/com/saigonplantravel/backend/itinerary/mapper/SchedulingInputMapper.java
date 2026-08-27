package com.saigonplantravel.backend.itinerary.mapper;

import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SchedulingInputMapper {

    public PlanningContext toPlanningContext(Trip trip) {
        return new PlanningContext(
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getBudget(),
                trip.getStartLatitude(),
                trip.getStartLongitude(),
                trip.getEnvironmentPreference());
    }

    public List<SchedulingCandidate> toSchedulingCandidates(
            Trip trip, List<RecommendationCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> new SchedulingCandidate(
                        toSchedulingPlace(candidate.place(), trip.getTripDate()),
                        candidate.semanticScore(),
                        candidate.matchedSection()))
                .toList();
    }

    public List<SchedulingPlace> toSchedulingPlaces(Trip trip, List<Place> places) {
        return places.stream()
                .map(place -> toSchedulingPlace(place, trip.getTripDate()))
                .toList();
    }

    private SchedulingPlace toSchedulingPlace(Place place, LocalDate tripDate) {
        return new SchedulingPlace(
                place.getSlug(),
                place.getName(),
                place.getLatitude(),
                place.getLongitude(),
                place.getEstimatedVisitMinutes(),
                place.getMinCost(),
                place.getIndoor(),
                place.getPrimaryImageUrl(),
                toOpeningWindow(place, tripDate));
    }

    private OpeningWindow toOpeningWindow(Place place, LocalDate tripDate) {
        short dayOfWeek = (short) tripDate.getDayOfWeek().getValue();

        OpeningHour openingHour = place.getOpeningHours().stream()
                .filter(hour -> hour.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);

        if (openingHour == null) {
            return OpeningWindow.unknown();
        }

        if (Boolean.TRUE.equals(openingHour.getClosed())) {
            return OpeningWindow.closed();
        }

        return OpeningWindow.open(openingHour.getOpenTime(), openingHour.getCloseTime());
    }
}
