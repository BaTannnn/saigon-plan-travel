package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.ScoredCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.ranking.CandidateRanker;
import com.saigonplantravel.backend.scheduling.scoring.CandidateScorer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItineraryScheduler {

    private final StopScheduleCalculator stopScheduleCalculator;
    private final CandidateScorer candidateScorer;
    private final CandidateRanker candidateRanker;

    public ItineraryScheduler(
            StopScheduleCalculator stopScheduleCalculator,
            CandidateScorer candidateScorer,
            CandidateRanker candidateRanker) {

        this.stopScheduleCalculator = stopScheduleCalculator;
        this.candidateScorer = candidateScorer;
        this.candidateRanker = candidateRanker;
    }

    public ItineraryPlan schedule(PlanningContext context, List<SchedulingCandidate> candidates) {

        List<SchedulingCandidate> remainingCandidates = new ArrayList<>(candidates);

        List<ScheduledStop> stops = new ArrayList<>();

        LocalDateTime currentDateTime = context.startDateTime();

        BigDecimal currentLatitude = context.startLatitude();

        BigDecimal currentLongitude = context.startLongitude();

        BigDecimal remainingBudget = context.budget();

        int totalTravelMinutes = 0;
        int totalVisitMinutes = 0;
        double totalDistanceKm = 0.0;

        while (!remainingCandidates.isEmpty()) {

            List<CandidateEvaluation> feasibleCandidates = new ArrayList<>();

            for (SchedulingCandidate candidate : remainingCandidates) {

                CandidateEvaluation evaluation = evaluateCandidate(
                        candidate, context, currentDateTime, currentLatitude, currentLongitude, remainingBudget);

                if (evaluation != null) {
                    feasibleCandidates.add(evaluation);
                }
            }

            if (feasibleCandidates.isEmpty()) {
                break;
            }

            List<ScoredCandidate> scoredCandidates = feasibleCandidates.stream()
                    .map(CandidateEvaluation::scoredCandidate)
                    .toList();

            ScoredCandidate bestCandidate =
                    candidateRanker.rank(scoredCandidates).getFirst();

            CandidateEvaluation selected = feasibleCandidates.stream()
                    .filter(evaluation -> evaluation
                            .scoredCandidate()
                            .place()
                            .slug()
                            .equals(bestCandidate.place().slug()))
                    .findFirst()
                    .orElseThrow();

            remainingCandidates.remove(selected.candidate());

            ScheduledStop selectedStop = selected.scheduledStop();

            stops.add(selectedStop);

            currentDateTime = selectedStop.visitEndDateTime();

            currentLatitude = selected.candidate().place().latitude();

            currentLongitude = selected.candidate().place().longitude();

            remainingBudget = remainingBudget.subtract(selectedStop.estimatedCost());

            totalTravelMinutes += selectedStop.travelMinutes();

            totalVisitMinutes += selected.candidate().place().estimatedVisitMinutes();

            totalDistanceKm += selectedStop.travelDistanceKm();
        }

        BigDecimal totalEstimatedCost = context.budget().subtract(remainingBudget);

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }

    private CandidateEvaluation evaluateCandidate(
            SchedulingCandidate candidate,
            PlanningContext context,
            LocalDateTime currentDateTime,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            BigDecimal remainingBudget) {

        SchedulingPlace place = candidate.place();

        // Cheap hard constraint first
        if (place.estimatedCost().compareTo(remainingBudget) > 0) {

            return null;
        }

        if (!place.openingWindow().isOpen()) {

            return null;
        }

        ScheduledStop scheduledStop =
                stopScheduleCalculator.calculate(context, currentDateTime, currentLatitude, currentLongitude, place);

        if (!scheduledStop.temporallyFeasible()) {

            return null;
        }

        ScoredCandidate scoredCandidate = candidateScorer.score(
                candidate,
                context,
                remainingBudget,
                scheduledStop.travelDistanceKm(),
                scheduledStop.travelMinutes());

        return new CandidateEvaluation(candidate, scoredCandidate, scheduledStop);
    }

    private record CandidateEvaluation(
            SchedulingCandidate candidate, ScoredCandidate scoredCandidate, ScheduledStop scheduledStop) {}
}
