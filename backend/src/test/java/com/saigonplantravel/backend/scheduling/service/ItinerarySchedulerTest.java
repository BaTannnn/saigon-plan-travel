package com.saigonplantravel.backend.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.ranking.CandidateRanker;
import com.saigonplantravel.backend.scheduling.scoring.BudgetScorer;
import com.saigonplantravel.backend.scheduling.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.scoring.EnvironmentScorer;
import com.saigonplantravel.backend.scheduling.scoring.TravelScorer;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItinerarySchedulerTest {

    private static final BigDecimal ORIGIN_LATITUDE = new BigDecimal("10.7700000");
    private static final BigDecimal ORIGIN_LONGITUDE = new BigDecimal("106.6900000");

    @Mock
    private TravelEstimator travelEstimator;

    private ItineraryScheduler scheduler;

    @BeforeEach
    void setUp() {
        CandidateScorer candidateScorer =
                new CandidateScorer(new TravelScorer(), new BudgetScorer(), new EnvironmentScorer());

        scheduler = new ItineraryScheduler(
                new StopScheduleCalculator(travelEstimator), candidateScorer, new CandidateRanker());
    }

    @Test
    void schedulesFeasibleCandidate() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace museum = openPlace(
                "museum", "10.7769000", "106.7009000", 90, "50000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.latitude(), museum.longitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        ItineraryPlan result = scheduler.schedule(context, List.of(candidate(museum, 0.90)));

        assertThat(result.stops()).hasSize(1);
        assertThat(result.stops().getFirst().place()).isEqualTo(museum);
        assertThat(result.stops().getFirst().arrivalTime()).isEqualTo(LocalTime.of(8, 10));
        assertThat(result.stops().getFirst().visitStartTime()).isEqualTo(LocalTime.of(8, 10));
        assertThat(result.stops().getFirst().visitEndTime()).isEqualTo(LocalTime.of(9, 40));
        assertThat(result.totalEstimatedCost()).isEqualByComparingTo("50000");
        assertThat(result.totalTravelMinutes()).isEqualTo(10);
        assertThat(result.totalVisitMinutes()).isEqualTo(90);
    }

    @Test
    void skipsClosedCandidateAndSchedulesNextFeasibleCandidate() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace closedPlace = place(
                "closed-place", "10.7750000", "106.6950000", 60, "0", OpeningWindow.closed());
        SchedulingPlace openPlace = openPlace(
                "open-place", "10.7760000", "106.6960000", 60, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, openPlace.latitude(), openPlace.longitude()))
                .thenReturn(new TravelEstimate(1.5, 8));

        ItineraryPlan result =
                scheduler.schedule(context, List.of(candidate(closedPlace, 0.95), candidate(openPlace, 0.80)));

        assertThat(result.stops()).hasSize(1);
        assertThat(result.stops().getFirst().place()).isEqualTo(openPlace);
    }

    @Test
    void skipsCandidateThatExceedsRemainingBudget() {
        PlanningContext context = context(new BigDecimal("100000"));
        SchedulingPlace expensivePlace = openPlace(
                "expensive", "10.7750000", "106.6950000", 60, "150000", LocalTime.of(8, 0), LocalTime.of(17, 0));
        SchedulingPlace affordablePlace = openPlace(
                "affordable", "10.7760000", "106.6960000", 60, "50000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, affordablePlace.latitude(), affordablePlace.longitude()))
                .thenReturn(new TravelEstimate(1.0, 5));

        ItineraryPlan result = scheduler.schedule(
                context, List.of(candidate(expensivePlace, 0.99), candidate(affordablePlace, 0.80)));

        assertThat(result.stops()).hasSize(1);
        assertThat(result.stops().getFirst().place()).isEqualTo(affordablePlace);
        assertThat(result.totalEstimatedCost()).isEqualByComparingTo("50000");
    }

    @Test
    void estimatesSecondTravelFromPreviouslySelectedPlace() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace firstPlace = openPlace(
                "first", "10.7750000", "106.6950000", 60, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));
        SchedulingPlace secondPlace = openPlace(
                "second", "10.7800000", "106.7050000", 60, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, firstPlace.latitude(), firstPlace.longitude()))
                .thenReturn(new TravelEstimate(1.0, 10));
        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, secondPlace.latitude(), secondPlace.longitude()))
                .thenReturn(new TravelEstimate(4.0, 30));
        when(travelEstimator.estimate(
                        firstPlace.latitude(), firstPlace.longitude(), secondPlace.latitude(), secondPlace.longitude()))
                .thenReturn(new TravelEstimate(2.0, 20));

        ItineraryPlan result =
                scheduler.schedule(context, List.of(candidate(firstPlace, 0.95), candidate(secondPlace, 0.85)));

        assertThat(result.stops()).hasSize(2);
        assertThat(result.stops()).extracting(stop -> stop.place().slug()).containsExactly("first", "second");
        assertThat(result.stops().get(0).visitEndTime()).isEqualTo(LocalTime.of(9, 10));
        assertThat(result.stops().get(1).arrivalTime()).isEqualTo(LocalTime.of(9, 30));
        verify(travelEstimator)
                .estimate(
                        firstPlace.latitude(), firstPlace.longitude(), secondPlace.latitude(), secondPlace.longitude());
    }

    @Test
    void waitsUntilOpeningTimeWhenArrivingEarly() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace museum = openPlace(
                "museum", "10.7769000", "106.7009000", 90, "50000", LocalTime.of(9, 0), LocalTime.of(17, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.latitude(), museum.longitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        ItineraryPlan result = scheduler.schedule(context, List.of(candidate(museum, 0.90)));

        assertThat(result.stops()).hasSize(1);
        assertThat(result.stops().getFirst().arrivalTime()).isEqualTo(LocalTime.of(8, 10));
        assertThat(result.stops().getFirst().visitStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.stops().getFirst().visitEndTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void skipsCandidateWhenVisitWouldEndAfterTripEndTime() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace longVisitPlace = openPlace(
                "long-visit", "10.7769000", "106.7009000", 700, "0", LocalTime.of(8, 0), LocalTime.of(22, 0));

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, longVisitPlace.latitude(), longVisitPlace.longitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        ItineraryPlan result = scheduler.schedule(context, List.of(candidate(longVisitPlace, 0.95)));

        assertThat(result.stops()).isEmpty();
        verify(travelEstimator)
                .estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, longVisitPlace.latitude(), longVisitPlace.longitude());
    }

    @Test
    void skipsMidnightOverflowCandidateWithoutAdvancingSchedulerIntoFakeNewDay() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace first = openPlace(
                "first", "10.7750000", "106.6950000", 470, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));
        SchedulingPlace overflow = openPlace(
                "overflow", "10.9000000", "106.9000000", 360, "0", LocalTime.of(8, 0), LocalTime.of(23, 59));
        SchedulingPlace later = openPlace(
                "later", "10.7800000", "106.7000000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, first.latitude(), first.longitude()))
                .thenReturn(new TravelEstimate(1.0, 10));
        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, overflow.latitude(), overflow.longitude()))
                .thenReturn(new TravelEstimate(50.0, 700));
        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, later.latitude(), later.longitude()))
                .thenReturn(new TravelEstimate(1.0, 10));
        when(travelEstimator.estimate(first.latitude(), first.longitude(), overflow.latitude(), overflow.longitude()))
                .thenReturn(new TravelEstimate(45.0, 164));
        when(travelEstimator.estimate(first.latitude(), first.longitude(), later.latitude(), later.longitude()))
                .thenReturn(new TravelEstimate(1.0, 10));
        when(travelEstimator.estimate(later.latitude(), later.longitude(), overflow.latitude(), overflow.longitude()))
                .thenReturn(new TravelEstimate(44.0, 164));

        ItineraryPlan result = scheduler.schedule(
                context, List.of(candidate(first, 1.0), candidate(overflow, 0.9), candidate(later, 0.0)));

        assertThat(result.stops()).extracting(stop -> stop.place().slug()).containsExactly("first", "later");
        assertThat(result.stops()).allSatisfy(stop -> {
            assertThat(stop.visitStartDateTime().toLocalDate()).isEqualTo(context.tripDate());
            assertThat(stop.visitEndDateTime()).isBeforeOrEqualTo(context.endDateTime());
            assertThat(stop.withinOpeningWindow()).isTrue();
        });
        assertThat(result.stops().get(1).visitStartDateTime())
                .isAfterOrEqualTo(result.stops().get(0).visitEndDateTime());
        assertThat(Duration.between(context.startDateTime(), result.stops().getLast().visitEndDateTime()))
                .isLessThanOrEqualTo(Duration.ofHours(10));
        assertThat(result.totalVisitMinutes()).isEqualTo(530);
        assertThat(result.totalTravelMinutes()).isEqualTo(20);
    }

    @Test
    void prefersBetterCurrentTravelTradeoffOverHigherSemanticScore() {
        PlanningContext context = context(new BigDecimal("500000"));
        SchedulingPlace farPlace = openPlace(
                "far", "10.8500000", "106.8000000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));
        SchedulingPlace nearPlace = openPlace(
                "near", "10.7710000", "106.6910000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, farPlace.latitude(), farPlace.longitude()))
                .thenReturn(new TravelEstimate(12.0, 50));
        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, nearPlace.latitude(), nearPlace.longitude()))
                .thenReturn(new TravelEstimate(1.0, 5));
        when(travelEstimator.estimate(
                        nearPlace.latitude(), nearPlace.longitude(), farPlace.latitude(), farPlace.longitude()))
                .thenReturn(new TravelEstimate(11.0, 50));

        ItineraryPlan result =
                scheduler.schedule(context, List.of(candidate(farPlace, 0.95), candidate(nearPlace, 0.90)));

        assertThat(result.stops()).isNotEmpty();
        assertThat(result.stops().getFirst().place().slug()).isEqualTo("near");
    }

    private PlanningContext context(BigDecimal budget) {
        return new PlanningContext(
                LocalDate.of(2026, 9, 2),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                budget,
                ORIGIN_LATITUDE,
                ORIGIN_LONGITUDE,
                EnvironmentPreference.MIXED);
    }

    private SchedulingPlace openPlace(
            String slug,
            String latitude,
            String longitude,
            int visitMinutes,
            String estimatedCost,
            LocalTime openTime,
            LocalTime closeTime) {
        return place(
                slug,
                latitude,
                longitude,
                visitMinutes,
                estimatedCost,
                OpeningWindow.open(openTime, closeTime));
    }

    private SchedulingPlace place(
            String slug,
            String latitude,
            String longitude,
            int visitMinutes,
            String estimatedCost,
            OpeningWindow openingWindow) {
        return new SchedulingPlace(
                slug,
                slug,
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                visitMinutes,
                new BigDecimal(estimatedCost),
                true,
                null,
                openingWindow);
    }

    private SchedulingCandidate candidate(SchedulingPlace place, double semanticScore) {
        return new SchedulingCandidate(place, semanticScore, "HIGHLIGHTS");
    }
}
