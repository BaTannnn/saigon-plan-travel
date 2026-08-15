package com.saigonplantravel.backend.recommendation.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator =
            new HaversineDistanceCalculator();

    @Test
    void returnsZeroForSameCoordinates() {

        double distance = calculator.calculateKm(
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"));

        assertThat(distance)
                .isEqualTo(0.0);
    }
    @Test
    void calculatesStraightLineDistanceBetweenTwoCoordinates() {

        double distance = calculator.calculateKm(
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                new BigDecimal("10.7769000"),
                new BigDecimal("106.7009000"));

        assertThat(distance)
                .isBetween(0.56, 0.58);
    }
    @Test
    void distanceIsSymmetric() {

        BigDecimal latitudeA =
                new BigDecimal("10.7726400");

        BigDecimal longitudeA =
                new BigDecimal("106.6980500");

        BigDecimal latitudeB =
                new BigDecimal("10.7769000");

        BigDecimal longitudeB =
                new BigDecimal("106.7009000");

        double fromAToB = calculator.calculateKm(
                latitudeA,
                longitudeA,
                latitudeB,
                longitudeB);

        double fromBToA = calculator.calculateKm(
                latitudeB,
                longitudeB,
                latitudeA,
                longitudeA);

        assertThat(fromAToB)
                .isEqualTo(fromBToA);
    }
}