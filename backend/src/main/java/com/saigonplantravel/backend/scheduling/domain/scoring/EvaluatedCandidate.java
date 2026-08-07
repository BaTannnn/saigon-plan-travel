package com.saigonplantravel.backend.scheduling.domain.scoring;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelEstimate;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitSchedule;

import java.util.Objects;

public record EvaluatedCandidate(
        PlaceSchedulingCandidate candidate,
        TravelEstimate travelEstimate,
        int adjustedVisitMinutes,
        VisitSchedule visitSchedule,
        CandidateScore score
) {

    public EvaluatedCandidate {
        Objects.requireNonNull(
                candidate,
                "candidate must not be null"
        );

        Objects.requireNonNull(
                travelEstimate,
                "travelEstimate must not be null"
        );

        Objects.requireNonNull(
                visitSchedule,
                "visitSchedule must not be null"
        );

        Objects.requireNonNull(
                score,
                "score must not be null"
        );

        if (adjustedVisitMinutes <= 0) {
            throw new IllegalArgumentException(
                    "adjustedVisitMinutes must be positive"
            );
        }
    }
}