package com.saigonplantravel.backend.itinerary.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItineraryIssueEvaluatorTest {

    private static final LocalTime TRIP_END = LocalTime.of(18, 0);

    private final ItineraryIssueEvaluator evaluator = new ItineraryIssueEvaluator();

    @Test
    void returnsNoIssuesForValidPlan() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace museum = openPlace("museum", 60, "50000", LocalTime.of(8, 0), LocalTime.of(17, 0));
        ScheduledStop stop = stop(museum, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        List<ItineraryIssue> result = evaluator.evaluate(context, plan(List.of(stop), new BigDecimal("50000")));

        assertThat(result).isEmpty();
    }

    @Test
    void reportsOverBudgetWithoutRemovingStops() {
        PlanningContext context = context(new BigDecimal("100000"));
        SchedulingPlace museum =
                openPlace("museum", 60, "150000", LocalTime.of(8, 0), LocalTime.of(17, 0));
        ScheduledStop stop = stop(museum, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));
        ItineraryPlan plan = plan(List.of(stop), new BigDecimal("150000"));

        List<ItineraryIssue> result = evaluator.evaluate(context, plan);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.OVER_BUDGET);
        assertThat(result.getFirst().placeSlug()).isNull();
        assertThat(plan.stops()).containsExactly(stop);
    }

    @Test
    void reportsClosedPlace() {
        SchedulingPlace closedPlace = place("closed-place", 60, "0", OpeningWindow.closed());
        ScheduledStop stop = stop(closedPlace, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        List<ItineraryIssue> result =
                evaluator.evaluate(context(new BigDecimal("500000")), plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.PLACE_CLOSED);
        assertThat(result.getFirst().placeSlug()).isEqualTo("closed-place");
    }

    @Test
    void reportsUnknownOpeningHours() {
        SchedulingPlace unknownPlace = place("unknown-hours", 60, "0", OpeningWindow.unknown());
        ScheduledStop stop = stop(unknownPlace, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        List<ItineraryIssue> result =
                evaluator.evaluate(context(new BigDecimal("500000")), plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.OPENING_HOURS_UNKNOWN);
        assertThat(result.getFirst().placeSlug()).isEqualTo("unknown-hours");
    }

    @Test
    void reportsWhenVisitEndsAfterPlaceClosingTime() {
        SchedulingPlace museum = openPlace("museum", 120, "0", LocalTime.of(8, 0), LocalTime.of(10, 0));
        ScheduledStop stop = stop(museum, LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(11, 0));

        List<ItineraryIssue> result =
                evaluator.evaluate(context(new BigDecimal("500000")), plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.ENDS_AFTER_CLOSING);
        assertThat(result.getFirst().placeSlug()).isEqualTo("museum");
    }

    @Test
    void reportsWhenVisitEndsAfterTripEndTime() {
        SchedulingPlace museum = openPlace("museum", 60, "0", LocalTime.of(8, 0), LocalTime.of(22, 0));
        ScheduledStop stop = stop(museum, LocalTime.of(17, 30), LocalTime.of(17, 30), LocalTime.of(18, 30));

        List<ItineraryIssue> result =
                evaluator.evaluate(context(new BigDecimal("500000")), plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.ENDS_AFTER_TRIP);
        assertThat(result.getFirst().placeSlug()).isEqualTo("museum");
    }

    @Test
    void reportsMultipleIndependentIssuesForSameStop() {
        SchedulingPlace museum = openPlace("museum", 120, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));
        ScheduledStop stop = stop(museum, LocalTime.of(16, 30), LocalTime.of(16, 30), LocalTime.of(18, 30));

        List<ItineraryIssue> result =
                evaluator.evaluate(context(new BigDecimal("500000")), plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result)
                .extracting(ItineraryIssue::type)
                .containsExactlyInAnyOrder(ItineraryIssueType.ENDS_AFTER_CLOSING, ItineraryIssueType.ENDS_AFTER_TRIP);
        assertThat(result).allSatisfy(issue -> assertThat(issue.placeSlug()).isEqualTo("museum"));
    }

    @Test
    void reportsPlanLevelAndPlaceLevelIssuesTogether() {
        PlanningContext context = context(new BigDecimal("100000"));
        SchedulingPlace museum =
                openPlace("museum", 60, "150000", LocalTime.of(8, 0), LocalTime.of(17, 0));
        ScheduledStop stop = stop(museum, LocalTime.of(17, 30), LocalTime.of(17, 30), LocalTime.of(18, 30));
        ItineraryPlan plan = plan(List.of(stop), new BigDecimal("150000"));

        List<ItineraryIssue> result = evaluator.evaluate(context, plan);

        assertThat(result)
                .extracting(ItineraryIssue::type)
                .containsExactlyInAnyOrder(
                        ItineraryIssueType.OVER_BUDGET,
                        ItineraryIssueType.ENDS_AFTER_CLOSING,
                        ItineraryIssueType.ENDS_AFTER_TRIP);
        assertThat(result).anySatisfy(issue -> {
            assertThat(issue.type()).isEqualTo(ItineraryIssueType.OVER_BUDGET);
            assertThat(issue.placeSlug()).isNull();
        });
        assertThat(result)
                .filteredOn(issue -> issue.placeSlug() != null)
                .allSatisfy(issue -> assertThat(issue.placeSlug()).isEqualTo("museum"));
    }

    private PlanningContext context(BigDecimal budget) {
        return new PlanningContext(
                LocalDate.of(2026, 9, 2),
                LocalTime.of(8, 0),
                TRIP_END,
                budget,
                new BigDecimal("10.7700000"),
                new BigDecimal("106.6900000"),
                EnvironmentPreference.MIXED);
    }

    private SchedulingPlace openPlace(
            String slug, int visitMinutes, String estimatedCost, LocalTime openTime, LocalTime closeTime) {
        return place(slug, visitMinutes, estimatedCost, OpeningWindow.open(openTime, closeTime));
    }

    private SchedulingPlace place(
            String slug, int visitMinutes, String estimatedCost, OpeningWindow openingWindow) {
        return new SchedulingPlace(
                slug,
                slug,
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                visitMinutes,
                new BigDecimal(estimatedCost),
                true,
                null,
                openingWindow);
    }

    private ScheduledStop stop(
            SchedulingPlace place,
            LocalTime arrivalTime,
            LocalTime visitStartTime,
            LocalTime visitEndTime) {
        boolean withinOpeningWindow = place.openingWindow().isOpen()
                && !visitEndTime.isAfter(place.openingWindow().closeTime());
        boolean withinTripWindow = !visitEndTime.isAfter(TRIP_END);

        return new ScheduledStop(
                place,
                LocalDateTime.of(LocalDate.of(2026, 9, 2), arrivalTime),
                LocalDateTime.of(LocalDate.of(2026, 9, 2), visitStartTime),
                LocalDateTime.of(LocalDate.of(2026, 9, 2), visitEndTime),
                10,
                1.0,
                place.estimatedCost(),
                withinOpeningWindow,
                withinTripWindow);
    }

    private ItineraryPlan plan(List<ScheduledStop> stops, BigDecimal totalEstimatedCost) {
        int totalTravelMinutes = stops.stream().mapToInt(ScheduledStop::travelMinutes).sum();
        int totalVisitMinutes =
                stops.stream().mapToInt(stop -> stop.place().estimatedVisitMinutes()).sum();
        double totalDistanceKm = stops.stream().mapToDouble(ScheduledStop::travelDistanceKm).sum();

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }
}
