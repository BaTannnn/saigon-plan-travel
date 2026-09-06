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
import org.junit.jupiter.api.Test;

class OpeningHoursCandidateFilterTest {

    private final OpeningHoursFeasibilityEvaluator evaluator = new OpeningHoursFeasibilityEvaluator();

    private final OpeningHoursCandidateFilter filter = new OpeningHoursCandidateFilter(evaluator);

    @Test
    void keepsOnlyCandidatesWithFeasibleOpeningHours() {

        Place feasiblePlace = place("Feasible", "feasible", 90);

        feasiblePlace.markOpen((short) 4, LocalTime.of(9, 0), LocalTime.of(17, 0));

        Place closedPlace = place("Closed", "closed", 90);

        closedPlace.markClosed((short) 4);

        Place unknownPlace = place("Unknown", "unknown", 90);

        List<RecommendationCandidate> candidates =
                List.of(candidate(feasiblePlace, 0.80), candidate(closedPlace, 0.95), candidate(unknownPlace, 0.90));

        Trip trip = createTripForThursday();

        List<RecommendationCandidate> result = filter.filter(candidates, trip);

        assertThat(result.stream().map(candidate -> candidate.place().getSlug()).toList())
                .containsExactly("feasible");
    }

    private Place place(String name, String slug, int estimatedVisitMinutes) {

        return new Place(
                name,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                estimatedVisitMinutes,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                true);
    }

    private RecommendationCandidate candidate(Place place, double semanticScore) {

        return new RecommendationCandidate(place, semanticScore, "HIGHLIGHTS");
    }

    private Trip createTripForThursday() {

        return new Trip(
                1L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                "Chợ Bến Thành",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                OffsetDateTime.parse("2026-08-13T10:00:00+07:00"));
    }
}
