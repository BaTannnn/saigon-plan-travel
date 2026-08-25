package com.saigonplantravel.backend.scheduling.scoring;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BudgetScorer {

    public double score(BigDecimal minCost, BigDecimal tripBudget) {

        validateNonNegative(minCost, "minCost");

        validateNonNegative(tripBudget, "tripBudget");

        if (tripBudget.signum() == 0) {
            return minCost.signum() == 0 ? 1.0 : 0.0;
        }

        double costRatio = minCost.doubleValue() / tripBudget.doubleValue();

        return 1.0 / (1.0 + costRatio);
    }

    private void validateNonNegative(BigDecimal value, String name) {

        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
