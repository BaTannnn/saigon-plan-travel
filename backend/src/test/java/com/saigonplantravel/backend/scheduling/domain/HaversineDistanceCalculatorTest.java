package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.scheduling.domain.travel.DistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.travel.GeoPoint;
import com.saigonplantravel.backend.scheduling.domain.travel.HaversineDistanceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.RoundingMode;
class HaversineDistanceCalculatorTest {

    private final DistanceCalculator calculator =
            new HaversineDistanceCalculator();

    @Test
    void returnsZeroForSamePoint() {
        GeoPoint point =
                new GeoPoint(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        BigDecimal distance =
                calculator.calculateKilometers(
                        point,
                        point
                );

        assertThat(distance)
                .isEqualByComparingTo(
                        "0.000"
                );
    }

    @Test
    void calculatesDistanceForOneLongitudeDegreeAtEquator() {
        GeoPoint from =
                new GeoPoint(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        GeoPoint to =
                new GeoPoint(
                        BigDecimal.ZERO,
                        BigDecimal.ONE
                );

        BigDecimal distance =
                calculator.calculateKilometers(
                        from,
                        to
                );

        assertThat(distance.setScale(3, RoundingMode.HALF_UP))
                .isEqualByComparingTo(
                        "111.195"
                );
    }
}