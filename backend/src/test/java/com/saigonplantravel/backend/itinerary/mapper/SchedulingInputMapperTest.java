package com.saigonplantravel.backend.itinerary.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulingInputMapperTest {

    private static final short WEDNESDAY = 3;
    private static final short THURSDAY = 4;

    private final SchedulingInputMapper mapper = new SchedulingInputMapper();

    @Test
    void mapsOnlyFieldsUsedByPlanningContext() {
        Trip trip = trip();

        PlanningContext result = mapper.toPlanningContext(trip);

        assertThat(result.tripDate()).isEqualTo(trip.getTripDate());
        assertThat(result.startTime()).isEqualTo(trip.getStartTime());
        assertThat(result.endTime()).isEqualTo(trip.getEndTime());
        assertThat(result.budget()).isEqualByComparingTo(trip.getBudget());
        assertThat(result.startLatitude()).isEqualByComparingTo(trip.getStartLatitude());
        assertThat(result.startLongitude()).isEqualByComparingTo(trip.getStartLongitude());
        assertThat(result.environmentPreference()).isEqualTo(EnvironmentPreference.INDOOR);
    }

    @Test
    void mapsRecommendationMetadataAndOpeningWindowForTripDay() {
        Trip trip = trip();
        Place museum = place("museum", 90, "50000", "75000");
        museum.markClosed(WEDNESDAY);
        museum.markOpen(THURSDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        RecommendationCandidate recommendation = new RecommendationCandidate(museum, 0.91, "HIGHLIGHTS");

        List<SchedulingCandidate> result = mapper.toSchedulingCandidates(trip, List.of(recommendation));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().semanticScore()).isEqualTo(0.91);
        assertThat(result.getFirst().matchedSection()).isEqualTo("HIGHLIGHTS");
        assertThat(result.getFirst().place().slug()).isEqualTo("museum");
        assertThat(result.getFirst().place().estimatedVisitMinutes()).isEqualTo(90);
        assertThat(result.getFirst().place().estimatedCost()).isEqualByComparingTo("50000");
        assertThat(result.getFirst().place().maxCost()).isEqualByComparingTo("75000");
        assertThat(result.getFirst().place().openingWindow())
                .isEqualTo(OpeningWindow.open(LocalTime.of(9, 0), LocalTime.of(17, 0)));
    }

    @Test
    void preservesManualOrderAndDistinguishesClosedFromUnknownHours() {
        Trip trip = trip();
        Place closed = place("closed", 60, "0");
        closed.markClosed(THURSDAY);
        Place unknown = place("unknown", 60, "0");

        List<SchedulingPlace> result = mapper.toSchedulingPlaces(trip, List.of(closed, unknown));

        assertThat(result).extracting(SchedulingPlace::slug).containsExactly("closed", "unknown");
        assertThat(result.get(0).openingWindow().status()).isEqualTo(OpeningWindow.Status.CLOSED);
        assertThat(result.get(1).openingWindow().status()).isEqualTo(OpeningWindow.Status.UNKNOWN);
    }

    private Trip trip() {
        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000"),
                "Test origin",
                new BigDecimal("10.7700000"),
                new BigDecimal("106.6900000"),
                TravelPace.BALANCED,
                EnvironmentPreference.INDOOR,
                OffsetDateTime.parse("2026-08-15T10:00:00+07:00"));
    }

    private Place place(String slug, int visitMinutes, String estimatedCost) {
        return place(slug, visitMinutes, estimatedCost, estimatedCost);
    }

    private Place place(String slug, int visitMinutes, String minCost, String maxCost) {
        return new Place(
                slug,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                visitMinutes,
                new BigDecimal(minCost),
                new BigDecimal(maxCost),
                true);
    }
}
