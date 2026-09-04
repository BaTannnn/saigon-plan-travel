package com.saigonplantravel.backend.scheduling.scoring;

import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScoredCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CandidateScorer {

    private static final double SEMANTIC_WEIGHT = 0.55;
    private static final double TRAVEL_WEIGHT = 0.20;
    private static final double BUDGET_WEIGHT = 0.15;
    private static final double ENVIRONMENT_WEIGHT = 0.10;

    private final TravelScorer travelScorer;
    private final BudgetScorer budgetScorer;
    private final EnvironmentScorer environmentScorer;

    public CandidateScorer(TravelScorer travelScorer, BudgetScorer budgetScorer, EnvironmentScorer environmentScorer) {

        this.travelScorer = travelScorer;
        this.budgetScorer = budgetScorer;
        this.environmentScorer = environmentScorer;
    }

    public ScoredCandidate score(
            SchedulingCandidate candidate,
            PlanningContext context,
            BigDecimal availableBudget,
            double travelDistanceKm,
            int travelMinutes) {

        double travelScore = travelScorer.score(travelMinutes);

        double budgetScore = budgetScorer.score(candidate.place().estimatedCost(), availableBudget);

        double environmentScore =
                environmentScorer.score(context.environmentPreference(), candidate.place().indoor());

        double finalScore = SEMANTIC_WEIGHT * candidate.semanticScore()
                + TRAVEL_WEIGHT * travelScore
                + BUDGET_WEIGHT * budgetScore
                + ENVIRONMENT_WEIGHT * environmentScore;

        return new ScoredCandidate(
                candidate.place(),
                candidate.matchedSection(),
                candidate.semanticScore(),
                travelDistanceKm,
                travelMinutes,
                travelScore,
                budgetScore,
                environmentScore,
                finalScore);
    }
}
