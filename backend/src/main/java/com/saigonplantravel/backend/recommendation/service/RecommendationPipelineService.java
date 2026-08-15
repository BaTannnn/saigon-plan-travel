package com.saigonplantravel.backend.recommendation.service;

import com.saigonplantravel.backend.recommendation.filter.BudgetCandidateFilter;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursCandidateFilter;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.ScoredCandidate;
import com.saigonplantravel.backend.recommendation.ranking.CandidateRanker;
import com.saigonplantravel.backend.recommendation.scoring.CandidateScorer;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationPipelineService {

    private final PlaceRecommendationService placeRecommendationService;
    private final OpeningHoursCandidateFilter openingHoursCandidateFilter;
    private final BudgetCandidateFilter budgetCandidateFilter;
    private final CandidateScorer candidateScorer;
    private final CandidateRanker candidateRanker;

    public RecommendationPipelineService(
            PlaceRecommendationService placeRecommendationService,
            OpeningHoursCandidateFilter openingHoursCandidateFilter,
            BudgetCandidateFilter budgetCandidateFilter,
            CandidateScorer candidateScorer,
            CandidateRanker candidateRanker) {

        this.placeRecommendationService = placeRecommendationService;
        this.openingHoursCandidateFilter = openingHoursCandidateFilter;
        this.budgetCandidateFilter = budgetCandidateFilter;
        this.candidateScorer = candidateScorer;
        this.candidateRanker = candidateRanker;
    }

    public List<ScoredCandidate> recommend(Trip trip, String preferenceDescription) {

        List<RecommendationCandidate> candidates = placeRecommendationService.recommend(preferenceDescription);

        candidates = openingHoursCandidateFilter.filter(candidates, trip);

        candidates = budgetCandidateFilter.filter(candidates, trip);

        List<ScoredCandidate> scoredCandidates = candidates.stream()
                .map(candidate -> candidateScorer.score(candidate, trip))
                .toList();

        return candidateRanker.rank(scoredCandidates);
    }
}
