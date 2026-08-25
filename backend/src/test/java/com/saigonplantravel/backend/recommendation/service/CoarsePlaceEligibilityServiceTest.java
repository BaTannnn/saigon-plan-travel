package com.saigonplantravel.backend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursFeasibilityEvaluator;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoarsePlaceEligibilityServiceTest {

    private static final short THURSDAY = 4;

    private final CoarsePlaceEligibilityService service =
            new CoarsePlaceEligibilityService(new OpeningHoursFeasibilityEvaluator());

    @Test
    void excludesKnownCostAboveWholeTripBudget() {
        assertThat(eligibleSlugs(place("expensive", "600000"))).isEmpty();
    }

    @Test
    void keepsKnownAffordableCost() {
        assertThat(eligibleSlugs(place("affordable", "500000"))).containsExactly("affordable");
    }

    @Test
    void keepsUnknownCost() {
        assertThat(eligibleSlugs(place("unknown-cost", null))).containsExactly("unknown-cost");
    }

    @Test
    void excludesPlaceClosedForRelevantDay() {
        Place place = place("closed", "0");
        place.markClosed(THURSDAY);

        assertThat(eligibleSlugs(place)).isEmpty();
    }

    @Test
    void excludesKnownOpenIntervalOutsideTripWindow() {
        Place place = place("afternoon-only", "0");
        place.markOpen(THURSDAY, LocalTime.of(14, 0), LocalTime.of(20, 0));

        assertThat(eligibleSlugs(place, morningTrip())).isEmpty();
    }

    @Test
    void excludesOverlapShorterThanEstimatedVisitDuration() {
        Place place = place("too-short", "0");
        place.markOpen(THURSDAY, LocalTime.of(11, 50), LocalTime.of(12, 0));

        assertThat(eligibleSlugs(place, morningTrip())).isEmpty();
    }

    @Test
    void keepsPlaceWhenOpenIntervalFitsEstimatedVisitDuration() {
        Place place = place("long-enough", "0");
        place.markOpen(THURSDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(eligibleSlugs(place, morningTrip())).containsExactly("long-enough");
    }

    @Test
    void keepsPlaceWithUnknownOpeningHours() {
        assertThat(eligibleSlugs(place("unknown-hours", "0"))).containsExactly("unknown-hours");
    }

    private List<String> eligibleSlugs(Place place) {
        return eligibleSlugs(place, trip(LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }

    private List<String> eligibleSlugs(Place place, Trip trip) {
        return service.findEligiblePlaces(List.of(place), trip).stream()
                .map(Place::getSlug)
                .toList();
    }

    private Place place(String slug, String minCost) {
        return new Place(
                slug,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                90,
                minCost == null ? null : new BigDecimal(minCost),
                new BigDecimal("1000000"),
                true);
    }

    private Trip morningTrip() {
        return trip(LocalTime.of(8, 0), LocalTime.of(12, 0));
    }

    private Trip trip(LocalTime startTime, LocalTime endTime) {
        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                startTime,
                endTime,
                new BigDecimal("500000"),
                "Chợ Bến Thành",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                OffsetDateTime.parse("2026-08-13T10:00:00+07:00"));
    }
}
