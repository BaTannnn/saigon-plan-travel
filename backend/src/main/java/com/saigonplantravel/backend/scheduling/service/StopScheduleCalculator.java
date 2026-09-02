package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class StopScheduleCalculator {

    private final TravelEstimator travelEstimator;

    public StopScheduleCalculator(TravelEstimator travelEstimator) {
        this.travelEstimator = travelEstimator;
    }

    public ScheduledStop calculate(
            PlanningContext context,
            LocalDateTime currentDateTime,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            SchedulingPlace place) {

        TravelEstimate travel = travelEstimator.estimate(
                currentLatitude, currentLongitude, place.latitude(), place.longitude());

        LocalDateTime arrivalDateTime = currentDateTime.plusMinutes(travel.estimatedMinutes());
        LocalDateTime visitStartDateTime = calculateVisitStartDateTime(context, arrivalDateTime, place.openingWindow());
        LocalDateTime visitEndDateTime = visitStartDateTime.plusMinutes(place.estimatedVisitMinutes());

        boolean withinOpeningWindow =
                isWithinOpeningWindow(context, place.openingWindow(), visitStartDateTime, visitEndDateTime);
        boolean withinTripWindow = !arrivalDateTime.isAfter(context.endDateTime())
                && !visitEndDateTime.isAfter(context.endDateTime());

        return new ScheduledStop(
                place,
                arrivalDateTime,
                visitStartDateTime,
                visitEndDateTime,
                travel.estimatedMinutes(),
                travel.estimatedDistanceKm(),
                place.estimatedCost(),
                withinOpeningWindow,
                withinTripWindow);
    }

    private LocalDateTime calculateVisitStartDateTime(
            PlanningContext context, LocalDateTime arrivalDateTime, OpeningWindow openingWindow) {
        if (!openingWindow.isOpen() || openingWindow.openTime() == null) {
            return arrivalDateTime;
        }

        LocalDateTime openDateTime = LocalDateTime.of(context.tripDate(), openingWindow.openTime());
        return arrivalDateTime.isAfter(openDateTime) ? arrivalDateTime : openDateTime;
    }

    private boolean isWithinOpeningWindow(
            PlanningContext context,
            OpeningWindow openingWindow,
            LocalDateTime visitStartDateTime,
            LocalDateTime visitEndDateTime) {
        if (!openingWindow.isOpen()
                || openingWindow.openTime() == null
                || openingWindow.closeTime() == null) {
            return false;
        }

        LocalDateTime openDateTime = LocalDateTime.of(context.tripDate(), openingWindow.openTime());
        LocalDateTime closeDateTime = LocalDateTime.of(context.tripDate(), openingWindow.closeTime());

        return !visitStartDateTime.isBefore(openDateTime) && !visitEndDateTime.isAfter(closeDateTime);
    }
}
