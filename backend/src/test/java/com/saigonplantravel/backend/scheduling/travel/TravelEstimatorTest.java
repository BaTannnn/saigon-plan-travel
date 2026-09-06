package com.saigonplantravel.backend.scheduling.travel;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TravelEstimatorTest {

    private final TravelEstimator estimator = new BaselineTravelEstimator(new HaversineDistanceCalculator());

    @Test
    void returnsZeroWhenLocationsAreTheSame() {

        TravelEstimate result = estimator.estimate(
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"),
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"));

        assertThat(result.estimatedDistanceKm()).isEqualTo(0.0);

        assertThat(result.estimatedMinutes()).isZero();
    }

    @Test
    void estimatesRouteDistanceAndTravelTime() {

        TravelEstimate result = estimator.estimate(
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"));

        assertThat(result.estimatedDistanceKm()).isBetween(0.73, 0.75);

        assertThat(result.estimatedMinutes()).isEqualTo(3);
    }
}
