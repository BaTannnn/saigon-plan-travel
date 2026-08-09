package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityEvaluator;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityInput;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityResult;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitRejectionReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class VisitFeasibilityEvaluatorTest {

    private static final LocalDate TRIP_DATE =
            LocalDate.of(
                    2026,
                    8,
                    9
            );

    private final VisitFeasibilityEvaluator evaluator =
            new VisitFeasibilityEvaluator();

    @Test
    void rejectsClosedPlace() {
        VisitFeasibilityInput input =
                new VisitFeasibilityInput(
                        at(8, 0),
                        at(17, 0),
                        20,
                        60,
                        OpeningHoursSnapshot.closed()
                );

        VisitFeasibilityResult result =
                evaluator.evaluate(input);

        assertThat(result.feasible())
                .isFalse();

        assertThat(result.rejectionReason())
                .isEqualTo(
                        VisitRejectionReason.PLACE_CLOSED
                );

        assertThat(result.schedule())
                .isNull();
    }

    @Test
    void schedulesUnknownOpeningHoursFromArrival() {
        VisitFeasibilityInput input =
                new VisitFeasibilityInput(
                        at(8, 0),
                        at(17, 0),
                        20,
                        60,
                        OpeningHoursSnapshot.unknown()
                );

        VisitFeasibilityResult result =
                evaluator.evaluate(input);

        assertThat(result.feasible())
                .isTrue();

        assertThat(result.schedule().arrivalAt())
                .isEqualTo(
                        at(8, 20)
                );

        assertThat(result.schedule().visitStartAt())
                .isEqualTo(
                        at(8, 20)
                );

        assertThat(result.schedule().visitEndAt())
                .isEqualTo(
                        at(9, 20)
                );
    }

    @Test
    void waitsUntilKnownOpeningTime() {
        VisitFeasibilityInput input =
                new VisitFeasibilityInput(
                        at(8, 0),
                        at(17, 0),
                        20,
                        60,
                        OpeningHoursSnapshot.knownOpen(
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0)
                        )
                );

        VisitFeasibilityResult result =
                evaluator.evaluate(input);

        assertThat(result.feasible())
                .isTrue();

        assertThat(result.schedule().arrivalAt())
                .isEqualTo(
                        at(8, 20)
                );

        assertThat(result.schedule().visitStartAt())
                .isEqualTo(
                        at(9, 0)
                );

        assertThat(result.schedule().visitEndAt())
                .isEqualTo(
                        at(10, 0)
                );

        assertThat(result.schedule().waitingMinutes())
                .isEqualTo(40);
    }

    @Test
    void reportsTripWindowBeforeClosingTimeWhenBothAreExceeded() {
        VisitFeasibilityInput input =
                new VisitFeasibilityInput(
                        at(16, 0),
                        at(17, 0),
                        0,
                        120,
                        OpeningHoursSnapshot.knownOpen(
                                LocalTime.of(8, 0),
                                LocalTime.of(16, 30)
                        )
                );

        VisitFeasibilityResult result = evaluator.evaluate(input);

        assertThat(result.feasible()).isFalse();
        assertThat(result.rejectionReason()).isEqualTo(
                VisitRejectionReason.TRIP_END_TIME_EXCEEDED
        );
    }

    private static LocalDateTime at(
            int hour,
            int minute
    ) {
        return LocalDateTime.of(
                TRIP_DATE,
                LocalTime.of(
                        hour,
                        minute
                )
        );
    }
}
