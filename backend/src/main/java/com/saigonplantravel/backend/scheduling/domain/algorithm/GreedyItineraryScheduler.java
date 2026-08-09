package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateRanker;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelTimeEstimator;
import com.saigonplantravel.backend.scheduling.domain.visit.PaceDurationPolicy;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GreedyItineraryScheduler {

    private final CandidateEvaluator candidateEvaluator;
    private final CandidateRanker candidateRanker;

    public GreedyItineraryScheduler(
            TravelTimeEstimator travelTimeEstimator,
            PaceDurationPolicy paceDurationPolicy,
            VisitFeasibilityEvaluator visitFeasibilityEvaluator,
            CandidateScorer candidateScorer,
            CandidateRanker candidateRanker
    ) {
        this.candidateEvaluator =
                new CandidateEvaluator(
                        Objects.requireNonNull(
                                travelTimeEstimator,
                                "travelTimeEstimator must not be null"
                        ),
                        Objects.requireNonNull(
                                paceDurationPolicy,
                                "paceDurationPolicy must not be null"
                        ),
                        Objects.requireNonNull(
                                visitFeasibilityEvaluator,
                                "visitFeasibilityEvaluator must not be null"
                        ),
                        Objects.requireNonNull(
                                candidateScorer,
                                "candidateScorer must not be null"
                        )
                );

        this.candidateRanker =
                Objects.requireNonNull(
                        candidateRanker,
                        "candidateRanker must not be null"
                );
    }

    public GreedySchedulingOutcome schedule(
            SchedulingInput input
    ) {
        Objects.requireNonNull(
                input,
                "input must not be null"
        );

        SchedulingState state =
                SchedulingState.initialize(
                        input
                );

        List<TerminalCandidateRejection>
                terminalRejections =
                List.of();

        while (!state
                .unscheduledCandidates()
                .isEmpty()) {

            CandidateBatchEvaluation batch =
                    evaluateCurrentState(
                            input,
                            state
                    );

            if (batch
                    .feasibleCandidates()
                    .isEmpty()) {

                terminalRejections =
                        batch.rejections();

                break;
            }

            EvaluatedCandidate bestCandidate =
                    candidateRanker
                            .selectBest(
                                    batch.feasibleCandidates()
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Candidate ranker returned no candidate"
                                            )
                            );

            state.applySelection(
                    bestCandidate
            );
        }

        return new GreedySchedulingOutcome(
                input.candidates().size(),
                state.scheduledCandidates(),
                terminalRejections,
                state.currentTime(),
                state.remainingBudget()
        );
    }

    private CandidateBatchEvaluation
    evaluateCurrentState(
            SchedulingInput input,
            SchedulingState state
    ) {
        List<EvaluatedCandidate>
                feasibleCandidates =
                new ArrayList<>();

        List<TerminalCandidateRejection>
                rejections =
                new ArrayList<>();

        for (
                PlaceSchedulingCandidate candidate
                : state.unscheduledCandidates()
        ) {
            CandidateEvaluationResult result =
                    candidateEvaluator.evaluate(
                            candidate,
                            state,
                            input.trip()
                    );

            if (result.feasible()) {
                feasibleCandidates.add(
                        result.evaluatedCandidate()
                );

                continue;
            }

            rejections.add(
                    new TerminalCandidateRejection(
                            candidate.placeId(),
                            result.rejectionReason()
                    )
            );
        }

        return new CandidateBatchEvaluation(
                feasibleCandidates,
                rejections
        );
    }

    private record CandidateBatchEvaluation(
            List<EvaluatedCandidate> feasibleCandidates,
            List<TerminalCandidateRejection> rejections
    ) {

        private CandidateBatchEvaluation {
            feasibleCandidates =
                    List.copyOf(
                            feasibleCandidates
                    );

            rejections =
                    List.copyOf(
                            rejections
                    );
        }
    }
}