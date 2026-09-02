package com.saigonplantravel.backend.recommendation.service;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceQueryService;
import com.saigonplantravel.backend.recommendation.SemanticPlaceRetriever;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.SemanticPlaceCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(PlaceRecommendationService.class);

    private static final int SEMANTIC_CANDIDATE_LIMIT = 30;

    private final SemanticPlaceRetriever semanticPlaceRetriever;
    private final PlaceQueryService placeQueryService;
    private final CoarsePlaceEligibilityService coarsePlaceEligibilityService;

    public PlaceRecommendationService(
            SemanticPlaceRetriever semanticPlaceRetriever,
            PlaceQueryService placeQueryService,
            CoarsePlaceEligibilityService coarsePlaceEligibilityService) {
        this.semanticPlaceRetriever = semanticPlaceRetriever;
        this.placeQueryService = placeQueryService;
        this.coarsePlaceEligibilityService = coarsePlaceEligibilityService;
    }

    public List<RecommendationCandidate> recommend(Trip trip, String preferenceDescription) {
        List<SemanticPlaceCandidate> semanticCandidates =
                semanticPlaceRetriever.retrieve(preferenceDescription.trim(), SEMANTIC_CANDIDATE_LIMIT);

        if (semanticCandidates.isEmpty()) {
            return List.of();
        }

        List<String> candidateSlugs = semanticCandidates.stream()
                .map(SemanticPlaceCandidate::placeSlug)
                .distinct()
                .toList();
        List<Place> candidatePlaces = placeQueryService.findAllActiveBySlugsForScheduling(candidateSlugs);
        List<Place> eligiblePlaces = coarsePlaceEligibilityService.findEligiblePlaces(candidatePlaces, trip);

        log.debug(
                "Coarse eligibility retained {} of {} semantic candidate places",
                eligiblePlaces.size(),
                candidatePlaces.size());

        Map<String, Place> placesBySlug =
                eligiblePlaces.stream().collect(Collectors.toMap(Place::getSlug, Function.identity()));

        return semanticCandidates.stream()
                .filter(candidate -> placesBySlug.containsKey(candidate.placeSlug()))
                .map(candidate -> new RecommendationCandidate(
                        placesBySlug.get(candidate.placeSlug()), candidate.semanticScore(), candidate.matchedSection()))
                .toList();
    }
}
