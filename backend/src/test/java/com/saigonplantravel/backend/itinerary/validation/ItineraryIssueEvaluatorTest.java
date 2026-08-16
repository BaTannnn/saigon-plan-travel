package com.saigonplantravel.backend.itinerary.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItineraryIssueEvaluatorTest {

    private static final Short THURSDAY = 4;

    private final ItineraryIssueEvaluator evaluator = new ItineraryIssueEvaluator();

    @Test
    void returnsNoIssuesForValidPlan() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum = openPlace("museum", 60, "50000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        ScheduledStop stop = stop(museum, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        ItineraryPlan plan = plan(List.of(stop), new BigDecimal("50000"));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan);

        assertThat(result).isEmpty();
    }

    @Test
    void reportsOverBudgetWithoutRemovingStops() {

        Trip trip = trip(new BigDecimal("100000"));

        Place museum = openPlace("museum", 60, "150000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        ScheduledStop stop = stop(museum, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        ItineraryPlan plan = plan(List.of(stop), new BigDecimal("150000"));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan);

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.OVER_BUDGET);

        assertThat(result.getFirst().placeSlug()).isNull();

        /*
         * Evaluator chỉ cảnh báo,
         * không được mutation itinerary.
         */
        assertThat(plan.stops()).containsExactly(stop);
    }

    @Test
    void reportsClosedPlace() {

        Trip trip = trip(new BigDecimal("500000"));

        Place closedPlace = place("closed-place", 60, "0");

        closedPlace.markClosed(THURSDAY);

        ScheduledStop stop = stop(closedPlace, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.PLACE_CLOSED);

        assertThat(result.getFirst().placeSlug()).isEqualTo("closed-place");
    }

    @Test
    void reportsUnknownOpeningHours() {

        Trip trip = trip(new BigDecimal("500000"));

        /*
         * Không gọi markOpen()/markClosed()
         * => Place không có opening-hour record
         * của THURSDAY.
         */
        Place unknownPlace = place("unknown-hours", 60, "0");

        ScheduledStop stop = stop(unknownPlace, LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 10));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.OPENING_HOURS_UNKNOWN);

        assertThat(result.getFirst().placeSlug()).isEqualTo("unknown-hours");
    }

    @Test
    void reportsWhenVisitEndsAfterPlaceClosingTime() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum = openPlace("museum", 120, "0", LocalTime.of(8, 0), LocalTime.of(10, 0));

        ScheduledStop stop = stop(museum, LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(11, 0));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.ENDS_AFTER_CLOSING);

        assertThat(result.getFirst().placeSlug()).isEqualTo("museum");
    }

    @Test
    void reportsWhenVisitEndsAfterTripEndTime() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum = openPlace("museum", 60, "0", LocalTime.of(8, 0), LocalTime.of(22, 0));

        ScheduledStop stop = stop(museum, LocalTime.of(17, 30), LocalTime.of(17, 30), LocalTime.of(18, 30));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().type()).isEqualTo(ItineraryIssueType.ENDS_AFTER_TRIP);

        assertThat(result.getFirst().placeSlug()).isEqualTo("museum");
    }

    @Test
    void reportsMultipleIndependentIssuesForSameStop() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum = openPlace("museum", 120, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));

        /*
         * visitEnd = 18:30
         *
         * Place closes = 17:00
         * Trip ends    = 18:00
         *
         * => vi phạm cả hai constraint.
         */
        ScheduledStop stop = stop(museum, LocalTime.of(16, 30), LocalTime.of(16, 30), LocalTime.of(18, 30));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan(List.of(stop), BigDecimal.ZERO));

        assertThat(result)
                .extracting(ItineraryIssue::type)
                .containsExactlyInAnyOrder(ItineraryIssueType.ENDS_AFTER_CLOSING, ItineraryIssueType.ENDS_AFTER_TRIP);

        assertThat(result).allSatisfy(issue -> assertThat(issue.placeSlug()).isEqualTo("museum"));
    }

    @Test
    void reportsPlanLevelAndPlaceLevelIssuesTogether() {

        Trip trip = trip(new BigDecimal("100000"));

        Place museum = openPlace("museum", 60, "150000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        ScheduledStop stop = stop(museum, LocalTime.of(17, 30), LocalTime.of(17, 30), LocalTime.of(18, 30));

        ItineraryPlan plan = plan(List.of(stop), new BigDecimal("150000"));

        List<ItineraryIssue> result = evaluator.evaluate(trip, plan);

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

    private Trip trip(BigDecimal budget) {

        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                budget,
                "Test origin",
                new BigDecimal("10.7700000"),
                new BigDecimal("106.6900000"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                OffsetDateTime.parse("2026-08-15T10:00:00+07:00"));
    }

    private Place openPlace(String slug, int visitMinutes, String minCost, LocalTime openTime, LocalTime closeTime) {

        Place place = place(slug, visitMinutes, minCost);

        place.markOpen(THURSDAY, openTime, closeTime);

        return place;
    }

    private Place place(String slug, int visitMinutes, String minCost) {

        BigDecimal cost = new BigDecimal(minCost);

        return new Place(
                slug,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                visitMinutes,
                cost,
                cost,
                true);
    }

    private ScheduledStop stop(Place place, LocalTime arrivalTime, LocalTime visitStartTime, LocalTime visitEndTime) {

        return new ScheduledStop(place, arrivalTime, visitStartTime, visitEndTime, 10, 1.0, place.getMinCost());
    }

    private ItineraryPlan plan(List<ScheduledStop> stops, BigDecimal totalEstimatedCost) {

        int totalTravelMinutes =
                stops.stream().mapToInt(ScheduledStop::travelMinutes).sum();

        int totalVisitMinutes = stops.stream()
                .mapToInt(stop -> stop.place().getEstimatedVisitMinutes())
                .sum();

        double totalDistanceKm =
                stops.stream().mapToDouble(ScheduledStop::travelDistanceKm).sum();

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }
}
