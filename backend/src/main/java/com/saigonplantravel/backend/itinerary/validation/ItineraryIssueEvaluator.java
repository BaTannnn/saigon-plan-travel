package com.saigonplantravel.backend.itinerary.validation;

import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItineraryIssueEvaluator {

    public List<ItineraryIssue> evaluate(PlanningContext context, ItineraryPlan plan) {

        List<ItineraryIssue> issues = new ArrayList<>();

        evaluateBudget(context, plan, issues);

        for (ScheduledStop stop : plan.stops()) {
            evaluateStop(stop, issues);
        }

        return List.copyOf(issues);
    }

    private void evaluateBudget(PlanningContext context, ItineraryPlan plan, List<ItineraryIssue> issues) {

        if (plan.totalEstimatedCost().compareTo(context.budget()) > 0) {

            issues.add(ItineraryIssue.forPlan(ItineraryIssueType.OVER_BUDGET));
        }
    }

    private void evaluateStop(ScheduledStop stop, List<ItineraryIssue> issues) {

        SchedulingPlace place = stop.place();

        OpeningWindow openingWindow = place.openingWindow();

        if (openingWindow.status() == OpeningWindow.Status.UNKNOWN) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.OPENING_HOURS_UNKNOWN, place.slug()));

        } else if (openingWindow.status() == OpeningWindow.Status.CLOSED) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.PLACE_CLOSED, place.slug()));

        } else if (!stop.withinOpeningWindow()) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.ENDS_AFTER_CLOSING, place.slug()));
        }

        if (!stop.withinTripWindow()) {

            issues.add(ItineraryIssue.forPlace(ItineraryIssueType.ENDS_AFTER_TRIP, place.slug()));
        }
    }
}
