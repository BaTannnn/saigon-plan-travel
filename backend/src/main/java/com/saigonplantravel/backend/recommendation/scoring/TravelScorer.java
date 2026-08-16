package com.saigonplantravel.backend.recommendation.scoring;

import org.springframework.stereotype.Component;

@Component
public class TravelScorer {

    private static final double REFERENCE_TRAVEL_MINUTES = 30.0;

    public double score(int estimatedMinutes) {

        if (estimatedMinutes < 0) {
            throw new IllegalArgumentException("estimatedMinutes must not be negative");
        }

        return 1.0 / (1.0 + estimatedMinutes / REFERENCE_TRAVEL_MINUTES);
    }
}
