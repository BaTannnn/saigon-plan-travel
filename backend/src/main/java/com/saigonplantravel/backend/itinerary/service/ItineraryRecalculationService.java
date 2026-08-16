package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.itinerary.model.CalculatedItinerary;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssue;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssueEvaluator;
import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ItineraryRecalculationService {

    private final TravelEstimator travelEstimator;
    private final ItineraryIssueEvaluator issueEvaluator;

    public ItineraryRecalculationService(TravelEstimator travelEstimator, ItineraryIssueEvaluator issueEvaluator) {

        this.travelEstimator = travelEstimator;
        this.issueEvaluator = issueEvaluator;
    }

    public CalculatedItinerary recalculate(Trip trip, List<Place> orderedPlaces) {

        ItineraryPlan plan = calculatePlan(trip, orderedPlaces);

        List<ItineraryIssue> issues = issueEvaluator.evaluate(trip, plan);

        return new CalculatedItinerary(plan, issues);
    }

    public ItineraryPlan calculatePlan(Trip trip, List<Place> orderedPlaces) {

        List<ScheduledStop> stops = new ArrayList<>();

        LocalTime currentTime = trip.getStartTime();

        BigDecimal currentLatitude = trip.getStartLatitude();

        BigDecimal currentLongitude = trip.getStartLongitude();

        BigDecimal totalEstimatedCost = BigDecimal.ZERO;

        int totalTravelMinutes = 0;
        int totalVisitMinutes = 0;
        double totalDistanceKm = 0.0;

        for (Place place : orderedPlaces) {

            TravelEstimate travel = travelEstimator.estimate(
                    currentLatitude, currentLongitude, place.getLatitude(), place.getLongitude());

            LocalTime arrivalTime = currentTime.plusMinutes(travel.estimatedMinutes());

            OpeningHour openingHour = findOpeningHour(place, trip);

            LocalTime visitStartTime = calculateVisitStartTime(arrivalTime, openingHour);

            LocalTime visitEndTime = visitStartTime.plusMinutes(place.getEstimatedVisitMinutes());

            ScheduledStop stop = new ScheduledStop(
                    place,
                    arrivalTime,
                    visitStartTime,
                    visitEndTime,
                    travel.estimatedMinutes(),
                    travel.estimatedDistanceKm(),
                    place.getMinCost());

            stops.add(stop);

            currentTime = visitEndTime;

            currentLatitude = place.getLatitude();

            currentLongitude = place.getLongitude();

            totalEstimatedCost = totalEstimatedCost.add(place.getMinCost());

            totalTravelMinutes += travel.estimatedMinutes();

            totalVisitMinutes += place.getEstimatedVisitMinutes();

            totalDistanceKm += travel.estimatedDistanceKm();
        }

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }

    private OpeningHour findOpeningHour(Place place, Trip trip) {

        short dayOfWeek = (short) trip.getTripDate().getDayOfWeek().getValue();

        return place.getOpeningHours().stream()
                .filter(openingHour -> openingHour.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);
    }

    private LocalTime calculateVisitStartTime(LocalTime arrivalTime, OpeningHour openingHour) {

        if (openingHour == null || Boolean.TRUE.equals(openingHour.getClosed()) || openingHour.getOpenTime() == null) {

            return arrivalTime;
        }

        return arrivalTime.isAfter(openingHour.getOpenTime()) ? arrivalTime : openingHour.getOpenTime();
    }
}
