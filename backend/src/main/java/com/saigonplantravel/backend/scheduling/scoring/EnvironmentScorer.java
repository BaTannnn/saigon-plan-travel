package com.saigonplantravel.backend.scheduling.scoring;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentScorer {

    public double score(EnvironmentPreference preference, boolean indoor) {

        return switch (preference) {
            case INDOOR -> indoor ? 1.0 : 0.0;
            case OUTDOOR -> indoor ? 0.0 : 1.0;
            case MIXED -> 1.0;
        };
    }
}
