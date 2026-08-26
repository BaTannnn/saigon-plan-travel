package com.saigonplantravel.backend.recommendation.service;

import com.saigonplantravel.backend.recommendation.filter.BudgetCandidateFilter;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursCandidateFilter;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationPipelineService {

    private final PlaceRecommendationService placeRecommendationService;
    private final OpeningHoursCandidateFilter openingHoursCandidateFilter;

    public RecommendationPipelineService(
            PlaceRecommendationService placeRecommendationService,
            OpeningHoursCandidateFilter openingHoursCandidateFilter,
            BudgetCandidateFilter budgetCandidateFilter) {

        this.placeRecommendationService = placeRecommendationService;
        this.openingHoursCandidateFilter = openingHoursCandidateFilter;
    }

    public List<RecommendationCandidate> recommend(Trip trip, String preferenceDescription) {

        List<RecommendationCandidate> candidates = placeRecommendationService.recommend(trip, preferenceDescription);

        candidates = openingHoursCandidateFilter.filter(candidates, trip);


        return candidates;
    }
}
