package com.saigonplantravel.backend.scheduling.domain;

import java.util.Objects;

public record VisitFeasibilityResult(
        boolean feasible,
        VisitSchedule schedule,
        VisitRejectionReason rejectionReason
) {

    public VisitFeasibilityResult {
        if (feasible) {
            Objects.requireNonNull(
                    schedule,
                    "schedule must not be null for a feasible result"
            );

            if (rejectionReason != null) {
                throw new IllegalArgumentException(
                        "rejectionReason must be null for a feasible result"
                );
            }
        } else {
            Objects.requireNonNull(
                    rejectionReason,
                    "rejectionReason must not be null for an infeasible result"
            );

            if (schedule != null) {
                throw new IllegalArgumentException(
                        "schedule must be null for an infeasible result"
                );
            }
        }
    }

    public static VisitFeasibilityResult feasible(
            VisitSchedule schedule
    ) {
        return new VisitFeasibilityResult(
                true,
                schedule,
                null
        );
    }

    public static VisitFeasibilityResult infeasible(
            VisitRejectionReason rejectionReason
    ) {
        return new VisitFeasibilityResult(
                false,
                null,
                rejectionReason
        );
    }
}