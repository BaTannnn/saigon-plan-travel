package com.saigonplantravel.backend.itinerary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.itinerary.model.CalculatedItinerary;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssue;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssueEvaluator;
import com.saigonplantravel.backend.itinerary.validation.ItineraryIssueType;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItineraryRecalculationServiceTest {

    private static final short THURSDAY = 4;

    private static final BigDecimal ORIGIN_LATITUDE = new BigDecimal("10.7700000");

    private static final BigDecimal ORIGIN_LONGITUDE = new BigDecimal("106.6900000");

    @Mock
    private TravelEstimator travelEstimator;

    @Mock
    private ItineraryIssueEvaluator issueEvaluator;

    private ItineraryRecalculationService service;

    @BeforeEach
    void setUp() {

        service = new ItineraryRecalculationService(travelEstimator, issueEvaluator);
    }

    @Test
    void recalculatesTimelineUsingUserDefinedOrder() {

        Trip trip = trip(new BigDecimal("500000"));

        Place first =
                openPlace("first", "10.7750000", "106.6950000", 60, "50000", LocalTime.of(8, 0), LocalTime.of(18, 0));

        Place second =
                openPlace("second", "10.7800000", "106.7050000", 90, "100000", LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, first.getLatitude(), first.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 10));

        when(travelEstimator.estimate(
                        first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude()))
                .thenReturn(new TravelEstimate(2.0, 20));

        when(issueEvaluator.evaluate(eq(trip), any(ItineraryPlan.class))).thenReturn(List.of());

        CalculatedItinerary result = service.recalculate(trip, List.of(first, second));

        ItineraryPlan plan = result.plan();

        assertThat(plan.stops()).hasSize(2);

        assertThat(plan.stops()).extracting(stop -> stop.place().getSlug()).containsExactly("first", "second");

        assertThat(plan.stops().get(0).arrivalTime()).isEqualTo(LocalTime.of(8, 10));

        assertThat(plan.stops().get(0).visitStartTime()).isEqualTo(LocalTime.of(8, 10));

        assertThat(plan.stops().get(0).visitEndTime()).isEqualTo(LocalTime.of(9, 10));

        assertThat(plan.stops().get(1).arrivalTime()).isEqualTo(LocalTime.of(9, 30));

        assertThat(plan.stops().get(1).visitStartTime()).isEqualTo(LocalTime.of(9, 30));

        assertThat(plan.stops().get(1).visitEndTime()).isEqualTo(LocalTime.of(11, 0));

        assertThat(plan.totalEstimatedCost()).isEqualByComparingTo("150000");

        assertThat(plan.totalTravelMinutes()).isEqualTo(30);

        assertThat(plan.totalVisitMinutes()).isEqualTo(150);

        assertThat(plan.totalDistanceKm()).isCloseTo(3.0, within(0.000001));

        assertThat(result.issues()).isEmpty();

        verify(travelEstimator)
                .estimate(first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude());

        verify(issueEvaluator).evaluate(eq(trip), eq(plan));
    }

    @Test
    void waitsUntilOpeningTimeWhenArrivingEarly() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum =
                openPlace("museum", "10.7769000", "106.7009000", 90, "50000", LocalTime.of(9, 0), LocalTime.of(17, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.getLatitude(), museum.getLongitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        when(issueEvaluator.evaluate(eq(trip), any(ItineraryPlan.class))).thenReturn(List.of());

        CalculatedItinerary result = service.recalculate(trip, List.of(museum));

        ItineraryPlan plan = result.plan();

        assertThat(plan.stops()).hasSize(1);

        assertThat(plan.stops().getFirst().arrivalTime()).isEqualTo(LocalTime.of(8, 10));

        assertThat(plan.stops().getFirst().visitStartTime()).isEqualTo(LocalTime.of(9, 0));

        assertThat(plan.stops().getFirst().visitEndTime()).isEqualTo(LocalTime.of(10, 30));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void preservesUserDefinedOrderWithoutRankingPlaces() {

        Trip trip = trip(new BigDecimal("500000"));

        Place first = openPlace("first", "10.7800000", "106.7050000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));

        Place second =
                openPlace("second", "10.7750000", "106.6950000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, first.getLatitude(), first.getLongitude()))
                .thenReturn(new TravelEstimate(4.0, 30));

        when(travelEstimator.estimate(
                        first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 5));

        when(issueEvaluator.evaluate(eq(trip), any(ItineraryPlan.class))).thenReturn(List.of());

        CalculatedItinerary result = service.recalculate(trip, List.of(first, second));

        assertThat(result.plan().stops())
                .extracting(stop -> stop.place().getSlug())
                .containsExactly("first", "second");
    }

    @Test
    void returnsCalculatedPlanWithDetectedIssues() {

        Trip trip = trip(new BigDecimal("100000"));

        Place museum =
                openPlace("museum", "10.7769000", "106.7009000", 60, "150000", LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.getLatitude(), museum.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 10));

        ItineraryIssue overBudget = ItineraryIssue.forPlan(ItineraryIssueType.OVER_BUDGET);

        when(issueEvaluator.evaluate(eq(trip), any(ItineraryPlan.class))).thenReturn(List.of(overBudget));

        CalculatedItinerary result = service.recalculate(trip, List.of(museum));

        assertThat(result.plan().stops()).hasSize(1);

        assertThat(result.plan().stops().getFirst().place()).isEqualTo(museum);

        assertThat(result.plan().totalEstimatedCost()).isEqualByComparingTo("150000");

        assertThat(result.issues()).containsExactly(overBudget);
    }

    @Test
    void returnsMultipleDetectedIssuesWithoutChangingPlan() {

        Trip trip = trip(new BigDecimal("100000"));

        Place museum =
                openPlace("museum", "10.7769000", "106.7009000", 60, "150000", LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.getLatitude(), museum.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 10));

        ItineraryIssue overBudget = ItineraryIssue.forPlan(ItineraryIssueType.OVER_BUDGET);

        ItineraryIssue closed = ItineraryIssue.forPlace(ItineraryIssueType.PLACE_CLOSED, "museum");

        when(issueEvaluator.evaluate(eq(trip), any(ItineraryPlan.class))).thenReturn(List.of(overBudget, closed));

        CalculatedItinerary result = service.recalculate(trip, List.of(museum));

        assertThat(result.plan().stops()).hasSize(1);

        assertThat(result.plan().stops().getFirst().place().getSlug()).isEqualTo("museum");

        assertThat(result.issues()).containsExactly(overBudget, closed);
    }

    @Test
    void returnsEmptyCalculatedItineraryForEmptyManualItinerary() {

        Trip trip = trip(new BigDecimal("500000"));

        when(issueEvaluator.evaluate(eq(trip), any(ItineraryPlan.class))).thenReturn(List.of());

        CalculatedItinerary result = service.recalculate(trip, List.of());

        ItineraryPlan plan = result.plan();

        assertThat(plan.stops()).isEmpty();

        assertThat(plan.totalEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(plan.totalTravelMinutes()).isZero();

        assertThat(plan.totalVisitMinutes()).isZero();

        assertThat(plan.totalDistanceKm()).isEqualTo(0.0);

        assertThat(result.issues()).isEmpty();

        verifyNoInteractions(travelEstimator);

        verify(issueEvaluator).evaluate(eq(trip), eq(plan));
    }

    private Trip trip(BigDecimal budget) {

        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                budget,
                "Test origin",
                ORIGIN_LATITUDE,
                ORIGIN_LONGITUDE,
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                OffsetDateTime.parse("2026-08-15T10:00:00+07:00"));
    }

    private Place openPlace(
            String slug,
            String latitude,
            String longitude,
            int visitMinutes,
            String minCost,
            LocalTime openTime,
            LocalTime closeTime) {

        Place place = place(slug, latitude, longitude, visitMinutes, minCost);

        place.markOpen(THURSDAY, openTime, closeTime);

        return place;
    }

    private Place place(String slug, String latitude, String longitude, int visitMinutes, String minCost) {

        BigDecimal cost = new BigDecimal(minCost);

        return new Place(
                slug,
                slug,
                "Ho Chi Minh City",
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                visitMinutes,
                cost,
                cost,
                true);
    }
}
