package com.saigonplantravel.backend.recommendation.filter;

import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpeningHoursCandidateFilter {

    private final OpeningHoursFeasibilityEvaluator evaluator;

    public OpeningHoursCandidateFilter(OpeningHoursFeasibilityEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public List<RecommendationCandidate> filter(List<RecommendationCandidate> candidates, Trip trip) {

        return candidates.stream()
                .filter(candidate -> isFeasible(candidate, trip))
                .toList();
    }

    private boolean isFeasible(RecommendationCandidate candidate, Trip trip) {

        OpeningHoursFeasibility feasibility =
                evaluator.evaluate(candidate.place(), trip.getTripDate(), trip.getStartTime(), trip.getEndTime());

        return feasibility == OpeningHoursFeasibility.FEASIBLE;
    }
}
