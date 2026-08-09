package com.saigonplantravel.backend.scheduling.domain.algorithm;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScore;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScoringInput;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.scheduling.domain.travel.GeoPoint;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelEstimate;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelTimeEstimator;
import com.saigonplantravel.backend.scheduling.domain.visit.PaceDurationPolicy;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityEvaluator;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityInput;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityResult;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitRejectionReason;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitSchedule;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;

import java.time.LocalDateTime;
import java.util.Objects;

final class CandidateEvaluator {

    private final TravelTimeEstimator travelTimeEstimator;
    private final PaceDurationPolicy paceDurationPolicy;
    private final VisitFeasibilityEvaluator visitFeasibilityEvaluator;
    private final CandidateScorer candidateScorer;

    CandidateEvaluator(
            TravelTimeEstimator travelTimeEstimator,
            PaceDurationPolicy paceDurationPolicy,
            VisitFeasibilityEvaluator visitFeasibilityEvaluator,
            CandidateScorer candidateScorer
    ) {
        this.travelTimeEstimator =
                Objects.requireNonNull(
                        travelTimeEstimator,
                        "travelTimeEstimator must not be null"
                );

        this.paceDurationPolicy =
                Objects.requireNonNull(
                        paceDurationPolicy,
                        "paceDurationPolicy must not be null"
                );

        this.visitFeasibilityEvaluator =
                Objects.requireNonNull(
                        visitFeasibilityEvaluator,
                        "visitFeasibilityEvaluator must not be null"
                );

        this.candidateScorer =
                Objects.requireNonNull(
                        candidateScorer,
                        "candidateScorer must not be null"
                );
    }

    CandidateEvaluationResult evaluate(
            PlaceSchedulingCandidate candidate,
            SchedulingState state,
            TripSchedulingSnapshot trip
    ) {
        Objects.requireNonNull(
                candidate,
                "candidate must not be null"
        );

        Objects.requireNonNull(
                state,
                "state must not be null"
        );

        Objects.requireNonNull(
                trip,
                "trip must not be null"
        );

        if (!matchesEnvironment(
                trip.environmentPreference(),
                candidate.indoor()
        )) {
            return CandidateEvaluationResult.rejected(
                    CandidateRejectionReason.ENVIRONMENT
            );
        }

        if (candidate.minCost()
                .compareTo(
                        state.remainingBudget()
                ) > 0) {
            return CandidateEvaluationResult.rejected(
                    CandidateRejectionReason.BUDGET
            );
        }

        if (candidate.openingHours().status()
                == OpeningHoursStatus.CLOSED) {
            return CandidateEvaluationResult.rejected(
                    CandidateRejectionReason.CLOSED_HOURS
            );
        }

        int adjustedVisitMinutes =
                paceDurationPolicy.adjustVisitMinutes(
                        candidate.baseVisitMinutes(),
                        trip.travelPace()
                );

        GeoPoint destination =
                new GeoPoint(
                        candidate.latitude(),
                        candidate.longitude()
                );

        TravelEstimate travelEstimate =
                travelTimeEstimator.estimate(
                        state.currentLocation(),
                        destination
                );

        LocalDateTime tripEndAt =
                LocalDateTime.of(
                        trip.tripDate(),
                        trip.endTime()
                );

        VisitFeasibilityInput feasibilityInput =
                new VisitFeasibilityInput(
                        state.currentTime(),
                        tripEndAt,
                        travelEstimate.travelMinutes(),
                        adjustedVisitMinutes,
                        candidate.openingHours()
                );

        VisitFeasibilityResult feasibilityResult =
                visitFeasibilityEvaluator.evaluate(
                        feasibilityInput
                );

        if (!feasibilityResult.feasible()) {
            return CandidateEvaluationResult.rejected(
                    mapRejectionReason(
                            feasibilityResult
                                    .rejectionReason()
                    )
            );
        }

        VisitSchedule visitSchedule =
                feasibilityResult.schedule();

        CandidateScoringInput scoringInput =
                new CandidateScoringInput(
                        candidate
                                .matchedPreferredCategoryIds()
                                .size(),
                        trip
                                .preferredCategoryIds()
                                .size(),
                        travelEstimate
                                .distanceKilometers(),
                        candidate.minCost(),
                        state.remainingBudget(),
                        candidate.openingHours().status(),
                        travelEstimate.travelMinutes(),
                        Math.toIntExact(
                                visitSchedule.waitingMinutes()
                        ),
                        adjustedVisitMinutes
                );

        CandidateScore score =
                candidateScorer.score(
                        scoringInput
                );

        EvaluatedCandidate evaluatedCandidate =
                new EvaluatedCandidate(
                        candidate,
                        travelEstimate,
                        adjustedVisitMinutes,
                        visitSchedule,
                        score
                );

        return CandidateEvaluationResult.feasible(
                evaluatedCandidate
        );
    }

    private static boolean matchesEnvironment(
            EnvironmentPreference preference,
            boolean indoor
    ) {
        return switch (preference) {
            case INDOOR ->
                    indoor;

            case OUTDOOR ->
                    !indoor;

            case MIXED ->
                    true;
        };
    }

    private static CandidateRejectionReason
    mapRejectionReason(
            VisitRejectionReason reason
    ) {
        return switch (reason) {
            case PLACE_CLOSED ->
                    CandidateRejectionReason
                            .CLOSED_HOURS;

            case TRIP_END_TIME_EXCEEDED ->
                    CandidateRejectionReason
                            .TRIP_WINDOW;

            case PLACE_CLOSING_TIME_EXCEEDED ->
                    CandidateRejectionReason
                            .CLOSING_TIME;
        };
    }
}