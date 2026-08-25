package com.saigonplantravel.backend.recommendation.service;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursFeasibility;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursFeasibilityEvaluator;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CoarsePlaceEligibilityService {

    private final OpeningHoursFeasibilityEvaluator openingHoursEvaluator;

    public CoarsePlaceEligibilityService(OpeningHoursFeasibilityEvaluator openingHoursEvaluator) {
        this.openingHoursEvaluator = openingHoursEvaluator;
    }

    public List<Place> findEligiblePlaces(List<Place> activePlaces, Trip trip) {
        return activePlaces.stream()
                .filter(place -> isWithinWholeTripBudget(place, trip.getBudget()))
                .filter(place -> hasFeasibleOpeningWindow(place, trip))
                .toList();
    }

    private boolean isWithinWholeTripBudget(Place place, BigDecimal tripBudget) {
        BigDecimal minCost = place.getMinCost();

        return minCost == null || tripBudget == null || minCost.compareTo(tripBudget) <= 0;
    }

    private boolean hasFeasibleOpeningWindow(Place place, Trip trip) {
        OpeningHoursFeasibility feasibility =
                openingHoursEvaluator.evaluate(place, trip.getTripDate(), trip.getStartTime(), trip.getEndTime());

        // Coarse filtering excludes only known-impossible places; unknown data remains eligible.
        return feasibility != OpeningHoursFeasibility.INFEASIBLE;
    }
}
