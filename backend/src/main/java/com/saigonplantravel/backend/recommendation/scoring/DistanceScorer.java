package com.saigonplantravel.backend.recommendation.scoring;

import org.springframework.stereotype.Component;

@Component
public class DistanceScorer {

    private static final double REFERENCE_DISTANCE_KM = 5.0;

    public double score(double distanceKm) {
        if (distanceKm < 0) {
            throw new IllegalArgumentException("distanceKm must not be negative");
        }

        return 1.0 / (1.0 + distanceKm / REFERENCE_DISTANCE_KM);
    }
}
