package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;

import java.util.Objects;

record CandidateEvaluationResult(
        EvaluatedCandidate evaluatedCandidate,
        CandidateRejectionReason rejectionReason
) {

    CandidateEvaluationResult {
        if (evaluatedCandidate == null
                && rejectionReason == null) {
            throw new IllegalArgumentException(
                    "evaluation result must contain candidate or rejection reason"
            );
        }

        if (evaluatedCandidate != null
                && rejectionReason != null) {
            throw new IllegalArgumentException(
                    "feasible candidate must not contain rejection reason"
            );
        }
    }

    static CandidateEvaluationResult feasible(
            EvaluatedCandidate candidate
    ) {
        return new CandidateEvaluationResult(
                Objects.requireNonNull(
                        candidate,
                        "candidate must not be null"
                ),
                null
        );
    }

    static CandidateEvaluationResult rejected(
            CandidateRejectionReason reason
    ) {
        return new CandidateEvaluationResult(
                null,
                Objects.requireNonNull(
                        reason,
                        "reason must not be null"
                )
        );
    }

    boolean feasible() {
        return evaluatedCandidate != null;
    }
}