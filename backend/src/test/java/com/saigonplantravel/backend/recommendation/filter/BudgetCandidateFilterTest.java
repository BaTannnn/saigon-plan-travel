package com.saigonplantravel.backend.recommendation.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BudgetCandidateFilterTest {

    private final BudgetCandidateFilter filter =
            new BudgetCandidateFilter();

    @Test
    void removesOnlyCandidateWhoseMinimumCostExceedsTripBudget() {

        RecommendationCandidate cheap =
                candidate(place("cheap", "100000"));

        RecommendationCandidate exact =
                candidate(place("exact", "500000"));

        RecommendationCandidate expensive =
                candidate(place("expensive", "700000"));

        Trip trip = tripWithBudget("500000");

        List<RecommendationCandidate> result =
                filter.filter(
                        List.of(
                                cheap,
                                exact,
                                expensive),
                        trip);

        assertThat(result)
                .extracting(candidate ->
                        candidate.place().getSlug())
                .containsExactly(
                        "cheap",
                        "exact");
    }

    @Test
    void zeroBudgetKeepsOnlyFreePlace() {

        RecommendationCandidate free =
                candidate(place("free", "0"));

        RecommendationCandidate paid =
                candidate(place("paid", "10000"));

        Trip trip = tripWithBudget("0");

        List<RecommendationCandidate> result =
                filter.filter(
                        List.of(free, paid),
                        trip);

        assertThat(result)
                .extracting(candidate ->
                        candidate.place().getSlug())
                .containsExactly("free");
    }

    private RecommendationCandidate candidate(
            Place place) {

        return new RecommendationCandidate(
                place,
                0.80,
                "HIGHLIGHTS");
    }

    private Place place(
            String slug,
            String minCost) {

        return new Place(
                slug,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                90,
                new BigDecimal(minCost),
                new BigDecimal("1000000"),
                true);
    }

    private Trip tripWithBudget(
            String budget) {

        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal(budget),
                "Chợ Bến Thành",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                Set.of(),
                OffsetDateTime.parse(
                        "2026-08-13T10:00:00+07:00"));
    }
}