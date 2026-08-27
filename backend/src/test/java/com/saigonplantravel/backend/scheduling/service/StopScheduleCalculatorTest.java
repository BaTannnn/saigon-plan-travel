package com.saigonplantravel.backend.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StopScheduleCalculatorTest {

    private static final BigDecimal FROM_LATITUDE = new BigDecimal("10.7700000");
    private static final BigDecimal FROM_LONGITUDE = new BigDecimal("106.6900000");
    private static final BigDecimal TO_LATITUDE = new BigDecimal("10.7769000");
    private static final BigDecimal TO_LONGITUDE = new BigDecimal("106.7009000");

    @Mock
    private TravelEstimator travelEstimator;

    private StopScheduleCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new StopScheduleCalculator(travelEstimator);
    }

    @Test
    void calculatesTravelArrivalAndVisitTimes() {
        SchedulingPlace place = place(
                60, OpeningWindow.open(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(travelEstimator.estimate(FROM_LATITUDE, FROM_LONGITUDE, TO_LATITUDE, TO_LONGITUDE))
                .thenReturn(new TravelEstimate(2.5, 10));

        ScheduledStop result =
                calculator.calculate(context(LocalTime.of(18, 0)), LocalTime.of(8, 0), FROM_LATITUDE, FROM_LONGITUDE, place);

        assertThat(result.arrivalTime()).isEqualTo(LocalTime.of(8, 10));
        assertThat(result.visitStartTime()).isEqualTo(LocalTime.of(8, 10));
        assertThat(result.visitEndTime()).isEqualTo(LocalTime.of(9, 10));
        assertThat(result.travelMinutes()).isEqualTo(10);
        assertThat(result.travelDistanceKm()).isEqualTo(2.5);
        assertThat(result.temporallyFeasible()).isTrue();
        verify(travelEstimator).estimate(FROM_LATITUDE, FROM_LONGITUDE, TO_LATITUDE, TO_LONGITUDE);
    }

    @Test
    void waitsUntilOpeningTimeWhenArrivalIsEarly() {
        SchedulingPlace place = place(
                90, OpeningWindow.open(LocalTime.of(9, 0), LocalTime.of(17, 0)));
        stubTravel(10);

        ScheduledStop result =
                calculator.calculate(context(LocalTime.of(18, 0)), LocalTime.of(8, 0), FROM_LATITUDE, FROM_LONGITUDE, place);

        assertThat(result.arrivalTime()).isEqualTo(LocalTime.of(8, 10));
        assertThat(result.visitStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.visitEndTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void startsAtArrivalForUnknownOrClosedHours() {
        stubTravel(10);
        ScheduledStop unknown = calculator.calculate(
                context(LocalTime.of(18, 0)),
                LocalTime.of(8, 0),
                FROM_LATITUDE,
                FROM_LONGITUDE,
                place(60, OpeningWindow.unknown()));
        ScheduledStop closed = calculator.calculate(
                context(LocalTime.of(18, 0)),
                LocalTime.of(8, 0),
                FROM_LATITUDE,
                FROM_LONGITUDE,
                place(60, OpeningWindow.closed()));

        assertThat(unknown.visitStartTime()).isEqualTo(unknown.arrivalTime());
        assertThat(closed.visitStartTime()).isEqualTo(closed.arrivalTime());
        assertThat(unknown.withinOpeningWindow()).isFalse();
        assertThat(closed.withinOpeningWindow()).isFalse();
    }

    @Test
    void treatsEndExactlyAtClosingAndTripEndAsFeasible() {
        SchedulingPlace place = place(
                60, OpeningWindow.open(LocalTime.of(8, 0), LocalTime.of(9, 10)));
        stubTravel(10);

        ScheduledStop result =
                calculator.calculate(context(LocalTime.of(9, 10)), LocalTime.of(8, 0), FROM_LATITUDE, FROM_LONGITUDE, place);

        assertThat(result.visitEndTime()).isEqualTo(LocalTime.of(9, 10));
        assertThat(result.withinOpeningWindow()).isTrue();
        assertThat(result.withinTripWindow()).isTrue();
        assertThat(result.temporallyFeasible()).isTrue();
    }

    @Test
    void reportsClosingAndTripConstraintsIndependently() {
        SchedulingPlace place = place(
                120, OpeningWindow.open(LocalTime.of(8, 0), LocalTime.of(9, 0)));
        stubTravel(10);

        ScheduledStop result =
                calculator.calculate(context(LocalTime.of(9, 30)), LocalTime.of(8, 0), FROM_LATITUDE, FROM_LONGITUDE, place);

        assertThat(result.visitEndTime()).isEqualTo(LocalTime.of(10, 10));
        assertThat(result.withinOpeningWindow()).isFalse();
        assertThat(result.withinTripWindow()).isFalse();
        assertThat(result.temporallyFeasible()).isFalse();
    }

    private void stubTravel(int minutes) {
        when(travelEstimator.estimate(FROM_LATITUDE, FROM_LONGITUDE, TO_LATITUDE, TO_LONGITUDE))
                .thenReturn(new TravelEstimate(1.0, minutes));
    }

    private PlanningContext context(LocalTime endTime) {
        return new PlanningContext(
                LocalTime.of(8, 0),
                endTime,
                new BigDecimal("500000"),
                FROM_LATITUDE,
                FROM_LONGITUDE,
                EnvironmentPreference.MIXED);
    }

    private SchedulingPlace place(int visitMinutes, OpeningWindow openingWindow) {
        return new SchedulingPlace(
                "museum",
                "Museum",
                TO_LATITUDE,
                TO_LONGITUDE,
                visitMinutes,
                new BigDecimal("50000"),
                true,
                null,
                openingWindow);
    }
}
