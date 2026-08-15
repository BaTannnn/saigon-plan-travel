package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.ScoredCandidate;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItineraryScheduler {

    private final TravelEstimator travelEstimator;

    public ItineraryScheduler(TravelEstimator travelEstimator) {

        this.travelEstimator = travelEstimator;
    }

    public ItineraryPlan schedule(Trip trip, List<ScoredCandidate> rankedCandidates) {

        List<ScoredCandidate> remainingCandidates = new ArrayList<>(rankedCandidates);

        List<ScheduledStop> stops = new ArrayList<>();

        LocalTime currentTime = trip.getStartTime();

        BigDecimal currentLatitude = trip.getStartLatitude();

        BigDecimal currentLongitude = trip.getStartLongitude();

        BigDecimal remainingBudget = trip.getBudget();

        int totalTravelMinutes = 0;
        int totalVisitMinutes = 0;
        double totalDistanceKm = 0.0;

        while (!remainingCandidates.isEmpty()) {

            int selectedIndex = -1;
            ScheduledStop selectedStop = null;

            for (int i = 0; i < remainingCandidates.size(); i++) {

                ScoredCandidate candidate = remainingCandidates.get(i);

                ScheduledStop stop =
                        trySchedule(candidate, trip, currentTime, currentLatitude, currentLongitude, remainingBudget);

                if (stop != null) {
                    selectedIndex = i;
                    selectedStop = stop;
                    break;
                }
            }

            if (selectedStop == null) {
                break;
            }

            ScoredCandidate selectedCandidate = remainingCandidates.remove(selectedIndex);

            stops.add(selectedStop);

            currentTime = selectedStop.visitEndTime();

            currentLatitude = selectedCandidate.place().getLatitude();

            currentLongitude = selectedCandidate.place().getLongitude();

            remainingBudget = remainingBudget.subtract(selectedStop.estimatedCost());

            totalTravelMinutes += selectedStop.travelMinutes();

            totalVisitMinutes += selectedCandidate.place().getEstimatedVisitMinutes();

            totalDistanceKm += selectedStop.travelDistanceKm();
        }

        BigDecimal totalEstimatedCost = trip.getBudget().subtract(remainingBudget);

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }

    private ScheduledStop trySchedule(
            ScoredCandidate candidate,
            Trip trip,
            LocalTime currentTime,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            BigDecimal remainingBudget) {

        Place place = candidate.place();

        if (place.getMinCost().compareTo(remainingBudget) > 0) {

            return null;
        }

        TravelEstimate travel =
                travelEstimator.estimate(currentLatitude, currentLongitude, place.getLatitude(), place.getLongitude());

        LocalTime arrivalTime = currentTime.plusMinutes(travel.estimatedMinutes());

        OpeningHour openingHour = findOpeningHour(place, trip);

        if (openingHour == null || Boolean.TRUE.equals(openingHour.getClosed())) {

            return null;
        }

        LocalTime visitStartTime = laterOf(arrivalTime, openingHour.getOpenTime());

        LocalTime visitEndTime = visitStartTime.plusMinutes(place.getEstimatedVisitMinutes());

        if (visitEndTime.isAfter(openingHour.getCloseTime())) {

            return null;
        }

        if (visitEndTime.isAfter(trip.getEndTime())) {

            return null;
        }

        return new ScheduledStop(
                place,
                arrivalTime,
                visitStartTime,
                visitEndTime,
                travel.estimatedMinutes(),
                travel.estimatedDistanceKm(),
                place.getMinCost(),
                candidate.finalScore());
    }

    private OpeningHour findOpeningHour(Place place, Trip trip) {

        short dayOfWeek = (short) trip.getTripDate().getDayOfWeek().getValue();

        return place.getOpeningHours().stream()
                .filter(openingHour -> openingHour.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);
    }

    private LocalTime laterOf(LocalTime first, LocalTime second) {

        return first.isAfter(second) ? first : second;
    }
}
