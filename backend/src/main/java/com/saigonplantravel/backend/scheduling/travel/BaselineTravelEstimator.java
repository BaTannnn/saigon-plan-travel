package com.saigonplantravel.backend.scheduling.travel;

import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BaselineTravelEstimator implements TravelEstimator {

    private static final double ROUTE_DISTANCE_FACTOR = 1.30;
    private static final double ASSUMED_SPEED_KMH = 20.0;

    private final HaversineDistanceCalculator distanceCalculator;

    public BaselineTravelEstimator(HaversineDistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public TravelEstimate estimate(
            BigDecimal fromLatitude, BigDecimal fromLongitude, BigDecimal toLatitude, BigDecimal toLongitude) {

        double straightLineDistanceKm =
                distanceCalculator.calculateKm(fromLatitude, fromLongitude, toLatitude, toLongitude);

        double estimatedDistanceKm = straightLineDistanceKm * ROUTE_DISTANCE_FACTOR;

        int estimatedMinutes = estimateMinutes(estimatedDistanceKm);

        return new TravelEstimate(estimatedDistanceKm, estimatedMinutes);
    }

    private int estimateMinutes(double distanceKm) {

        if (distanceKm == 0.0) {
            return 0;
        }

        return (int) Math.ceil(distanceKm / ASSUMED_SPEED_KMH * 60.0);
    }
}
