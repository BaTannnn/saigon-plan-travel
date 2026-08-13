package com.saigonplantravel.backend.recommendation.service;

import com.saigonplantravel.backend.ai.client.AiRecommendationClient;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceCandidateResponse;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationResponse;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceRecommendationService {

    private final AiRecommendationClient aiRecommendationClient;
    private final PlaceRepository placeRepository;

    public PlaceRecommendationService(
            AiRecommendationClient aiRecommendationClient,
            PlaceRepository placeRepository) {
        this.aiRecommendationClient = aiRecommendationClient;
        this.placeRepository = placeRepository;
    }

    public List<RecommendationCandidate> recommend(
            String query,
            int candidateLimit) {

        AiPlaceRecommendationResponse aiResponse =
                aiRecommendationClient.recommendPlaces(
                        query,
                        candidateLimit);

        List<AiPlaceCandidateResponse> aiCandidates =
                aiResponse.candidates();

        if (aiCandidates.isEmpty()) {
            return List.of();
        }

        List<String> slugs = aiCandidates.stream()
                .map(AiPlaceCandidateResponse::placeSlug)
                .toList();

        Map<String, Place> placesBySlug =
                placeRepository
                        .findAllBySlugInAndActiveTrue(slugs)
                        .stream()
                        .collect(Collectors.toMap(
                                Place::getSlug,
                                Function.identity()));

        return aiCandidates.stream()
                .filter(candidate ->
                        placesBySlug.containsKey(
                                candidate.placeSlug()))
                .map(candidate ->
                        new RecommendationCandidate(
                                placesBySlug.get(
                                        candidate.placeSlug()),
                                candidate.semanticScore(),
                                candidate.matchedSection()))
                .toList();
    }
}