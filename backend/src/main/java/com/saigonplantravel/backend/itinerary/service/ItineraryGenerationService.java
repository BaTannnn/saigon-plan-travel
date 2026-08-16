package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.service.RecommendationPipelineService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.service.ItineraryScheduler;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryGenerationService {

    private final TripRepository tripRepository;
    private final RecommendationPipelineService recommendationPipelineService;
    private final ItineraryScheduler itineraryScheduler;

    public ItineraryGenerationService(
            TripRepository tripRepository,
            RecommendationPipelineService recommendationPipelineService,
            ItineraryScheduler itineraryScheduler) {

        this.tripRepository = tripRepository;
        this.recommendationPipelineService = recommendationPipelineService;

        this.itineraryScheduler = itineraryScheduler;
    }

    @Transactional(readOnly = true)
    public ItineraryPlan generatePlan(Long userId, UUID tripPublicId, String preferenceDescription) {

        Trip trip =
                tripRepository.findByPublicIdAndUserId(tripPublicId, userId).orElseThrow(TripNotFoundException::new);

        List<RecommendationCandidate> candidates = recommendationPipelineService.recommend(trip, preferenceDescription);

        return itineraryScheduler.schedule(trip, candidates);
    }
}
