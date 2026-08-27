package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class StopScheduleCalculator {

    private final TravelEstimator travelEstimator;

    public StopScheduleCalculator(TravelEstimator travelEstimator) {
        this.travelEstimator = travelEstimator;
    }

    public ScheduledStop calculate(
            PlanningContext context,
            LocalTime currentTime,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            SchedulingPlace place) {

        TravelEstimate travel = travelEstimator.estimate(
                currentLatitude, currentLongitude, place.latitude(), place.longitude());

        LocalTime arrivalTime = currentTime.plusMinutes(travel.estimatedMinutes());
        LocalTime visitStartTime = calculateVisitStartTime(arrivalTime, place.openingWindow());
        LocalTime visitEndTime = visitStartTime.plusMinutes(place.estimatedVisitMinutes());

        boolean withinOpeningWindow = isWithinOpeningWindow(place.openingWindow(), visitEndTime);
        boolean withinTripWindow = !visitEndTime.isAfter(context.endTime());

        return new ScheduledStop(
                place,
                arrivalTime,
                visitStartTime,
                visitEndTime,
                travel.estimatedMinutes(),
                travel.estimatedDistanceKm(),
                place.estimatedCost(),
                withinOpeningWindow,
                withinTripWindow);
    }

    private LocalTime calculateVisitStartTime(LocalTime arrivalTime, OpeningWindow openingWindow) {
        if (!openingWindow.isOpen() || openingWindow.openTime() == null) {
            return arrivalTime;
        }

        return arrivalTime.isAfter(openingWindow.openTime()) ? arrivalTime : openingWindow.openTime();
    }

    private boolean isWithinOpeningWindow(OpeningWindow openingWindow, LocalTime visitEndTime) {
        return openingWindow.isOpen()
                && openingWindow.closeTime() != null
                && !visitEndTime.isAfter(openingWindow.closeTime());
    }
}
