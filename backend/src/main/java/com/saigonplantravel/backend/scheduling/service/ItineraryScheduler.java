package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.ScoredCandidate;
import com.saigonplantravel.backend.recommendation.ranking.CandidateRanker;
import com.saigonplantravel.backend.recommendation.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import com.saigonplantravel.backend.scheduling.travel.TravelEstimator;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItineraryScheduler {

    private final TravelEstimator travelEstimator;
    private final CandidateScorer candidateScorer;
    private final CandidateRanker candidateRanker;

    public ItineraryScheduler(
            TravelEstimator travelEstimator, CandidateScorer candidateScorer, CandidateRanker candidateRanker) {

        this.travelEstimator = travelEstimator;
        this.candidateScorer = candidateScorer;
        this.candidateRanker = candidateRanker;
    }

    public ItineraryPlan schedule(Trip trip, List<RecommendationCandidate> candidates) {

        List<RecommendationCandidate> remainingCandidates = new ArrayList<>(candidates);

        List<ScheduledStop> stops = new ArrayList<>();

        LocalTime currentTime = trip.getStartTime();

        BigDecimal currentLatitude = trip.getStartLatitude();

        BigDecimal currentLongitude = trip.getStartLongitude();

        BigDecimal remainingBudget = trip.getBudget();

        int totalTravelMinutes = 0;
        int totalVisitMinutes = 0;
        double totalDistanceKm = 0.0;

        while (!remainingCandidates.isEmpty()) {

            List<CandidateEvaluation> feasibleCandidates = new ArrayList<>();

            for (RecommendationCandidate candidate : remainingCandidates) {

                CandidateEvaluation evaluation = evaluateCandidate(
                        candidate, trip, currentTime, currentLatitude, currentLongitude, remainingBudget);

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
                            .getSlug()
                            .equals(bestCandidate.place().getSlug()))
                    .findFirst()
                    .orElseThrow();

            remainingCandidates.remove(selected.candidate());

            ScheduledStop selectedStop = selected.scheduledStop();

            stops.add(selectedStop);

            currentTime = selectedStop.visitEndTime();

            currentLatitude = selected.candidate().place().getLatitude();

            currentLongitude = selected.candidate().place().getLongitude();

            remainingBudget = remainingBudget.subtract(selectedStop.estimatedCost());

            totalTravelMinutes += selectedStop.travelMinutes();

            totalVisitMinutes += selected.candidate().place().getEstimatedVisitMinutes();

            totalDistanceKm += selectedStop.travelDistanceKm();
        }

        BigDecimal totalEstimatedCost = trip.getBudget().subtract(remainingBudget);

        return new ItineraryPlan(stops, totalEstimatedCost, totalTravelMinutes, totalVisitMinutes, totalDistanceKm);
    }

    private CandidateEvaluation evaluateCandidate(
            RecommendationCandidate candidate,
            Trip trip,
            LocalTime currentTime,
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            BigDecimal remainingBudget) {

        Place place = candidate.place();

        // Cheap hard constraint first
        if (place.getMinCost().compareTo(remainingBudget) > 0) {

            return null;
        }

        OpeningHour openingHour = findOpeningHour(place, trip);

        if (openingHour == null || Boolean.TRUE.equals(openingHour.getClosed())) {

            return null;
        }

        TravelEstimate travel =
                travelEstimator.estimate(currentLatitude, currentLongitude, place.getLatitude(), place.getLongitude());

        LocalTime arrivalTime = currentTime.plusMinutes(travel.estimatedMinutes());

        LocalTime visitStartTime = laterOf(arrivalTime, openingHour.getOpenTime());

        LocalTime visitEndTime = visitStartTime.plusMinutes(place.getEstimatedVisitMinutes());

        if (visitEndTime.isAfter(openingHour.getCloseTime())) {

            return null;
        }

        if (visitEndTime.isAfter(trip.getEndTime())) {

            return null;
        }

        ScoredCandidate scoredCandidate = candidateScorer.score(
                candidate, trip, remainingBudget, travel.estimatedDistanceKm(), travel.estimatedMinutes());

        ScheduledStop scheduledStop = new ScheduledStop(
                place,
                arrivalTime,
                visitStartTime,
                visitEndTime,
                travel.estimatedMinutes(),
                travel.estimatedDistanceKm(),
                place.getMinCost(),
                scoredCandidate.finalScore());

        return new CandidateEvaluation(candidate, scoredCandidate, scheduledStop);
    }

    private OpeningHour findOpeningHour(Place place, Trip trip) {

        short dayOfWeek = (short) trip.getTripDate().getDayOfWeek().getValue();

        return place.getOpeningHours().stream()
                .filter(openingHour -> openingHour.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);
    }

    private LocalTime laterOf(LocalTime first, LocalTime second) {

        return first.isAfter(second) ? first : second;
    }

    private record CandidateEvaluation(
            RecommendationCandidate candidate, ScoredCandidate scoredCandidate, ScheduledStop scheduledStop) {}
}
