package com.saigonplantravel.backend.itinerary.validation;

import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItineraryIssueEvaluator {

    public List<ItineraryIssue> evaluate(Trip trip, ItineraryPlan plan) {

        List<ItineraryIssue> issues = new ArrayList<>();

        evaluateBudget(trip, plan, issues);

        for (ScheduledStop stop : plan.stops()) {
            evaluateStop(trip, stop, issues);
        }

        return List.copyOf(issues);
    }

    private void evaluateBudget(Trip trip, ItineraryPlan plan, List<ItineraryIssue> issues) {

        if (plan.totalEstimatedCost().compareTo(trip.getBudget()) > 0) {

            issues.add(ItineraryIssue.forPlan(ItineraryIssueType.OVER_BUDGET));
        }
    }

    private void evaluateStop(Trip trip, ScheduledStop stop, List<ItineraryIssue> issues) {

        Place place = stop.place();

        OpeningHour openingHour = findOpeningHour(place, trip);

        if (openingHour == null) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.OPENING_HOURS_UNKNOWN, place.getSlug()));

        } else if (Boolean.TRUE.equals(openingHour.getClosed())) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.PLACE_CLOSED, place.getSlug()));

        } else if (stop.visitEndTime().isAfter(openingHour.getCloseTime())) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.ENDS_AFTER_CLOSING, place.getSlug()));
        }

        if (stop.visitEndTime().isAfter(trip.getEndTime())) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.ENDS_AFTER_TRIP, place.getSlug()));
        }
    }

    private OpeningHour findOpeningHour(Place place, Trip trip) {

        short dayOfWeek = (short) trip.getTripDate().getDayOfWeek().getValue();

        return place.getOpeningHours().stream()
                .filter(openingHour -> openingHour.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);
    }
}
