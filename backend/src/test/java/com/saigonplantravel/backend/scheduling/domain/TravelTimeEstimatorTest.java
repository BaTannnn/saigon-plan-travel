package com.saigonplantravel.backend.scheduling.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TravelTimeEstimatorTest {

    @Test
    void estimatesTravelMinutesAndRoundsUp() {
        DistanceCalculator distanceCalculator =
                (from, to) ->
                        new BigDecimal("1.000");

        SchedulingPolicy schedulingPolicy =
                new SchedulingPolicy(
                        SchedulingAlgorithmVersion.GREEDY_V1,
                        new BigDecimal("18.00"),
                        5,
                        100
                );

        TravelTimeEstimator estimator =
                new TravelTimeEstimator(
                        distanceCalculator,
                        schedulingPolicy
                );

        GeoPoint point =
                new GeoPoint(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        TravelEstimate estimate =
                estimator.estimate(
                        point,
                        point
                );

        assertThat(
                estimate.distanceKilometers()
        ).isEqualByComparingTo(
                "1.000"
        );

        assertThat(
                estimate.travelMinutes()
        ).isEqualTo(
                9
        );
    }
}