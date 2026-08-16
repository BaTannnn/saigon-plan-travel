package com.saigonplantravel.backend.recommendation.filter;

import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BudgetCandidateFilter {

    public List<RecommendationCandidate> filter(List<RecommendationCandidate> candidates, Trip trip) {

        BigDecimal tripBudget = trip.getBudget();

        return candidates.stream()
                .filter(candidate -> fitsBudget(candidate, tripBudget))
                .toList();
    }

    private boolean fitsBudget(RecommendationCandidate candidate, BigDecimal tripBudget) {

        BigDecimal minCost = candidate.place().getMinCost();

        return minCost.compareTo(tripBudget) <= 0;
    }
}
