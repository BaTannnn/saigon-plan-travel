package com.saigonplantravel.backend.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.ranking.CandidateRanker;
import com.saigonplantravel.backend.recommendation.scoring.BudgetScorer;
import com.saigonplantravel.backend.recommendation.scoring.CandidateScorer;
import com.saigonplantravel.backend.recommendation.scoring.EnvironmentScorer;
import com.saigonplantravel.backend.recommendation.scoring.TravelScorer;
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
class ItinerarySchedulerTest {

    private static final short THURSDAY = 4;

    private static final BigDecimal ORIGIN_LATITUDE = new BigDecimal("10.7700000");

    private static final BigDecimal ORIGIN_LONGITUDE = new BigDecimal("106.6900000");

    @Mock
    private TravelEstimator travelEstimator;

    private ItineraryScheduler scheduler;

    @BeforeEach
    void setUp() {

        CandidateScorer candidateScorer =
                new CandidateScorer(new TravelScorer(), new BudgetScorer(), new EnvironmentScorer());

        CandidateRanker candidateRanker = new CandidateRanker();

        scheduler = new ItineraryScheduler(travelEstimator, candidateScorer, candidateRanker);
    }

    @Test
    void schedulesFeasibleCandidate() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum =
                openPlace("museum", "10.7769000", "106.7009000", 90, "50000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        RecommendationCandidate candidate = candidate(museum, 0.90);

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.getLatitude(), museum.getLongitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        ItineraryPlan result = scheduler.schedule(trip, List.of(candidate));

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

        Trip trip = trip(new BigDecimal("500000"));

        Place closedPlace = place("closed-place", "10.7750000", "106.6950000", 60, "0");

        closedPlace.markClosed(THURSDAY);

        Place openPlace =
                openPlace("open-place", "10.7760000", "106.6960000", 60, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));

        RecommendationCandidate closedCandidate = candidate(closedPlace, 0.95);

        RecommendationCandidate openCandidate = candidate(openPlace, 0.80);

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, openPlace.getLatitude(), openPlace.getLongitude()))
                .thenReturn(new TravelEstimate(1.5, 8));

        ItineraryPlan result = scheduler.schedule(trip, List.of(closedCandidate, openCandidate));

        assertThat(result.stops()).hasSize(1);

        assertThat(result.stops().getFirst().place()).isEqualTo(openPlace);
    }

    @Test
    void skipsCandidateThatExceedsRemainingBudget() {

        Trip trip = trip(new BigDecimal("100000"));

        Place expensivePlace = openPlace(
                "expensive", "10.7750000", "106.6950000", 60, "150000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        Place affordablePlace = openPlace(
                "affordable", "10.7760000", "106.6960000", 60, "50000", LocalTime.of(8, 0), LocalTime.of(17, 0));

        RecommendationCandidate expensiveCandidate = candidate(expensivePlace, 0.99);

        RecommendationCandidate affordableCandidate = candidate(affordablePlace, 0.80);

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE,
                        ORIGIN_LONGITUDE,
                        affordablePlace.getLatitude(),
                        affordablePlace.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 5));

        ItineraryPlan result = scheduler.schedule(trip, List.of(expensiveCandidate, affordableCandidate));

        assertThat(result.stops()).hasSize(1);

        assertThat(result.stops().getFirst().place()).isEqualTo(affordablePlace);

        assertThat(result.totalEstimatedCost()).isEqualByComparingTo("50000");
    }

    @Test
    void estimatesSecondTravelFromPreviouslySelectedPlace() {

        Trip trip = trip(new BigDecimal("500000"));

        Place firstPlace =
                openPlace("first", "10.7750000", "106.6950000", 60, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));

        Place secondPlace =
                openPlace("second", "10.7800000", "106.7050000", 60, "0", LocalTime.of(8, 0), LocalTime.of(17, 0));

        RecommendationCandidate firstCandidate = candidate(firstPlace, 0.95);

        RecommendationCandidate secondCandidate = candidate(secondPlace, 0.85);

        // Vòng 1: V2 evaluate CẢ HAI từ origin.
        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, firstPlace.getLatitude(), firstPlace.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 10));

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, secondPlace.getLatitude(), secondPlace.getLongitude()))
                .thenReturn(new TravelEstimate(4.0, 30));

        // Vòng 2: sau khi first thắng,
        // second phải được evaluate lại từ first.
        when(travelEstimator.estimate(
                        firstPlace.getLatitude(),
                        firstPlace.getLongitude(),
                        secondPlace.getLatitude(),
                        secondPlace.getLongitude()))
                .thenReturn(new TravelEstimate(2.0, 20));

        ItineraryPlan result = scheduler.schedule(trip, List.of(firstCandidate, secondCandidate));

        assertThat(result.stops()).hasSize(2);

        assertThat(result.stops()).extracting(stop -> stop.place().getSlug()).containsExactly("first", "second");

        assertThat(result.stops().get(0).visitEndTime()).isEqualTo(LocalTime.of(9, 10));

        assertThat(result.stops().get(1).arrivalTime()).isEqualTo(LocalTime.of(9, 30));

        verify(travelEstimator)
                .estimate(
                        firstPlace.getLatitude(),
                        firstPlace.getLongitude(),
                        secondPlace.getLatitude(),
                        secondPlace.getLongitude());
    }

    @Test
    void waitsUntilOpeningTimeWhenArrivingEarly() {

        Trip trip = trip(new BigDecimal("500000"));

        Place museum =
                openPlace("museum", "10.7769000", "106.7009000", 90, "50000", LocalTime.of(9, 0), LocalTime.of(17, 0));

        RecommendationCandidate candidate = candidate(museum, 0.90);

        when(travelEstimator.estimate(ORIGIN_LATITUDE, ORIGIN_LONGITUDE, museum.getLatitude(), museum.getLongitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        ItineraryPlan result = scheduler.schedule(trip, List.of(candidate));

        assertThat(result.stops()).hasSize(1);

        assertThat(result.stops().getFirst().arrivalTime()).isEqualTo(LocalTime.of(8, 10));

        assertThat(result.stops().getFirst().visitStartTime()).isEqualTo(LocalTime.of(9, 0));

        assertThat(result.stops().getFirst().visitEndTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void skipsCandidateWhenVisitWouldEndAfterTripEndTime() {

        Trip trip = trip(new BigDecimal("500000"));

        Place longVisitPlace =
                openPlace("long-visit", "10.7769000", "106.7009000", 700, "0", LocalTime.of(8, 0), LocalTime.of(22, 0));

        RecommendationCandidate candidate = candidate(longVisitPlace, 0.95);

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, longVisitPlace.getLatitude(), longVisitPlace.getLongitude()))
                .thenReturn(new TravelEstimate(2.0, 10));

        ItineraryPlan result = scheduler.schedule(trip, List.of(candidate));

        assertThat(result.stops()).isEmpty();

        verify(travelEstimator)
                .estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, longVisitPlace.getLatitude(), longVisitPlace.getLongitude());
    }

    @Test
    void prefersBetterCurrentTravelTradeoffOverHigherSemanticScore() {

        Trip trip = trip(new BigDecimal("500000"));

        Place farPlace =
                openPlace("far", "10.8500000", "106.8000000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));

        Place nearPlace =
                openPlace("near", "10.7710000", "106.6910000", 60, "0", LocalTime.of(8, 0), LocalTime.of(18, 0));

        RecommendationCandidate farCandidate = candidate(farPlace, 0.95);

        RecommendationCandidate nearCandidate = candidate(nearPlace, 0.90);

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, farPlace.getLatitude(), farPlace.getLongitude()))
                .thenReturn(new TravelEstimate(12.0, 50));

        when(travelEstimator.estimate(
                        ORIGIN_LATITUDE, ORIGIN_LONGITUDE, nearPlace.getLatitude(), nearPlace.getLongitude()))
                .thenReturn(new TravelEstimate(1.0, 5));

        // Sau khi near được chọn, far vẫn còn.
        // Scheduler sẽ evaluate far lại từ near.
        when(travelEstimator.estimate(
                        nearPlace.getLatitude(),
                        nearPlace.getLongitude(),
                        farPlace.getLatitude(),
                        farPlace.getLongitude()))
                .thenReturn(new TravelEstimate(11.0, 50));

        ItineraryPlan result = scheduler.schedule(trip, List.of(farCandidate, nearCandidate));

        assertThat(result.stops()).isNotEmpty();

        assertThat(result.stops().getFirst().place().getSlug()).isEqualTo("near");
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

    private RecommendationCandidate candidate(Place place, double semanticScore) {

        return new RecommendationCandidate(place, semanticScore, "HIGHLIGHTS");
    }
}
