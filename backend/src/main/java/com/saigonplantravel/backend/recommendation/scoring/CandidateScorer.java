package com.saigonplantravel.backend.recommendation.scoring;

import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.ScoredCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import org.springframework.stereotype.Component;

@Component
public class CandidateScorer {

    private static final double SEMANTIC_WEIGHT = 0.55;
    private static final double DISTANCE_WEIGHT = 0.20;
    private static final double BUDGET_WEIGHT = 0.15;
    private static final double ENVIRONMENT_WEIGHT = 0.10;

    private final HaversineDistanceCalculator distanceCalculator;
    private final DistanceScorer distanceScorer;
    private final BudgetScorer budgetScorer;
    private final EnvironmentScorer environmentScorer;

    public CandidateScorer(
            HaversineDistanceCalculator distanceCalculator,
            DistanceScorer distanceScorer,
            BudgetScorer budgetScorer,
            EnvironmentScorer environmentScorer) {

        this.distanceCalculator = distanceCalculator;
        this.distanceScorer = distanceScorer;
        this.budgetScorer = budgetScorer;
        this.environmentScorer = environmentScorer;
    }

    public ScoredCandidate score(RecommendationCandidate candidate, Trip trip) {

        double distanceKm = distanceCalculator.calculateKm(
                trip.getStartLatitude(),
                trip.getStartLongitude(),
                candidate.place().getLatitude(),
                candidate.place().getLongitude());

        double distanceScore = distanceScorer.score(distanceKm);

        double budgetScore = budgetScorer.score(candidate.place().getMinCost(), trip.getBudget());

        double environmentScore = environmentScorer.score(
                trip.getEnvironmentPreference(), candidate.place().getIndoor());

        double finalScore = SEMANTIC_WEIGHT * candidate.semanticScore()
                + DISTANCE_WEIGHT * distanceScore
                + BUDGET_WEIGHT * budgetScore
                + ENVIRONMENT_WEIGHT * environmentScore;

        return new ScoredCandidate(
                candidate.place(),
                candidate.matchedSection(),
                candidate.semanticScore(),
                distanceKm,
                distanceScore,
                budgetScore,
                environmentScore,
                finalScore);
    }
}
