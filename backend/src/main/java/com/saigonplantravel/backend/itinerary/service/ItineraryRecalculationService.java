package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.itinerary.mapper.SchedulingInputMapper;
import com.saigonplantravel.backend.itinerary.model.CalculatedItinerary;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssue;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssueEvaluator;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.service.StopScheduleCalculator;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ItineraryRecalculationService {

    private final SchedulingInputMapper schedulingInputMapper;
    private final StopScheduleCalculator stopScheduleCalculator;
    private final ItineraryIssueEvaluator issueEvaluator;

    public ItineraryRecalculationService(
            SchedulingInputMapper schedulingInputMapper,
            StopScheduleCalculator stopScheduleCalculator,
            ItineraryIssueEvaluator issueEvaluator) {

        this.schedulingInputMapper = schedulingInputMapper;
        this.stopScheduleCalculator = stopScheduleCalculator;
        this.issueEvaluator = issueEvaluator;
    }

    public CalculatedItinerary recalculate(Trip trip, List<Place> orderedPlaces) {

        PlanningContext context = schedulingInputMapper.toPlanningContext(trip);
        List<SchedulingPlace> schedulingPlaces = schedulingInputMapper.toSchedulingPlaces(trip, orderedPlaces);
        ItineraryPlan plan = calculatePlan(context, schedulingPlaces);

        List<ItineraryIssue> issues = issueEvaluator.evaluate(context, plan);

        return new CalculatedItinerary(plan, issues);
    }

    private ItineraryPlan calculatePlan(PlanningContext context, List<SchedulingPlace> orderedPlaces) {

        List<ScheduledStop> stops = new ArrayList<>();

        LocalTime currentTime = context.startTime();

        BigDecimal currentLatitude = context.startLatitude();

        BigDecimal currentLongitude = context.startLongitude();

        BigDecimal totalEstimatedCost = BigDecimal.ZERO;

        int totalTravelMinutes = 0;
        int totalVisitMinutes = 0;
        double totalDistanceKm = 0.0;

        for (SchedulingPlace place : orderedPlaces) {

            ScheduledStop stop =
                    stopScheduleCalculator.calculate(context, currentTime, currentLatitude, currentLongitude, place);

            stops.add(stop);

            currentTime = stop.visitEndTime();

            currentLatitude = place.latitude();

            currentLongitude = place.longitude();

            totalEstimatedCost = totalEstimatedCost.add(place.estimatedCost());

            totalTravelMinutes += stop.travelMinutes();

            totalVisitMinutes += place.estimatedVisitMinutes();

            totalDistanceKm += stop.travelDistanceKm();
        }

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }
}
