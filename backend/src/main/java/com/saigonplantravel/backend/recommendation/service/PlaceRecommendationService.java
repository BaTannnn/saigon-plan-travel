package com.saigonplantravel.backend.recommendation.service;

import com.saigonplantravel.backend.ai.client.AiRecommendationClient;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceCandidateResponse;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationResponse;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceQueryService;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
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

    private static final int SEMANTIC_CANDIDATE_LIMIT = 15;

    private final AiRecommendationClient aiRecommendationClient;
    private final PlaceQueryService placeQueryService;
    private final CoarsePlaceEligibilityService coarsePlaceEligibilityService;

    public PlaceRecommendationService(
            AiRecommendationClient aiRecommendationClient,
            PlaceQueryService placeQueryService,
            CoarsePlaceEligibilityService coarsePlaceEligibilityService) {
        this.aiRecommendationClient = aiRecommendationClient;
        this.placeQueryService = placeQueryService;
        this.coarsePlaceEligibilityService = coarsePlaceEligibilityService;
    }

    public List<RecommendationCandidate> recommend(Trip trip, String preferenceDescription) {
        List<Place> activePlaces = placeQueryService.findAllActiveForScheduling();
        List<Place> eligiblePlaces = coarsePlaceEligibilityService.findEligiblePlaces(activePlaces, trip);

        log.debug("Coarse eligibility retained {} of {} active places", eligiblePlaces.size(), activePlaces.size());

        if (eligiblePlaces.isEmpty()) {
            return List.of();
        }

        List<String> eligiblePlaceSlugs =
                eligiblePlaces.stream().map(Place::getSlug).toList();

        AiPlaceRecommendationResponse aiResponse = aiRecommendationClient.recommendPlaces(
                preferenceDescription.trim(), SEMANTIC_CANDIDATE_LIMIT, eligiblePlaceSlugs);

        List<AiPlaceCandidateResponse> aiCandidates = aiResponse.candidates();

        log.debug("AI returned {} candidates from {} eligible places", aiCandidates.size(), eligiblePlaces.size());

        if (aiCandidates.isEmpty()) {
            return List.of();
        }

        Map<String, Place> placesBySlug =
                eligiblePlaces.stream().collect(Collectors.toMap(Place::getSlug, Function.identity()));

        return aiCandidates.stream()
                .filter(candidate -> placesBySlug.containsKey(candidate.placeSlug()))
                .map(candidate -> new RecommendationCandidate(
                        placesBySlug.get(candidate.placeSlug()), candidate.semanticScore(), candidate.matchedSection()))
                .toList();
    }
}
