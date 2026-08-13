package com.saigonplantravel.backend.recommendation.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.place.entity.Place;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class OpeningHoursFeasibilityEvaluatorTest {

    private final OpeningHoursFeasibilityEvaluator evaluator =
            new OpeningHoursFeasibilityEvaluator();

    @Test
    void returnsFeasibleWhenVisitFitsInsideAvailableWindow() {
        Place place = placeWithVisitDuration(90);

        place.markOpen(
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0));

        OpeningHoursFeasibility result = evaluator.evaluate(
                place,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0));

        assertThat(result)
                .isEqualTo(OpeningHoursFeasibility.FEASIBLE);
    }

    @Test
    void returnsInfeasibleWhenPlaceIsClosed() {
        Place place = placeWithVisitDuration(90);

        place.markClosed((short) 1);

        OpeningHoursFeasibility result = evaluator.evaluate(
                place,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0));

        assertThat(result)
                .isEqualTo(OpeningHoursFeasibility.INFEASIBLE);
    }

    @Test
    void returnsInfeasibleWhenAvailableWindowIsTooShort() {
        Place place = placeWithVisitDuration(90);

        place.markOpen(
                (short) 1,
                LocalTime.of(17, 0),
                LocalTime.of(18, 0));

        OpeningHoursFeasibility result = evaluator.evaluate(
                place,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0));

        assertThat(result)
                .isEqualTo(OpeningHoursFeasibility.INFEASIBLE);
    }

    @Test
    void returnsUnknownWhenOpeningHoursAreMissing() {
        Place place = placeWithVisitDuration(90);

        OpeningHoursFeasibility result = evaluator.evaluate(
                place,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0));

        assertThat(result)
                .isEqualTo(OpeningHoursFeasibility.UNKNOWN);
    }

    private Place placeWithVisitDuration(int minutes) {
        return new Place(
                "Test Place",
                "test-place",
                "Ho Chi Minh City",
                new BigDecimal("10.7769"),
                new BigDecimal("106.7009"),
                minutes,
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                true);
    }
}