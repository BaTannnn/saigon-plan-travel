package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.ai.client.AiItineraryExplanationClient;
import com.saigonplantravel.backend.itinerary.mapper.SchedulingInputMapper;
import com.saigonplantravel.backend.itinerary.model.GeneratedItineraryPreview;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.service.RecommendationPipelineService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.service.ItineraryScheduler;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.service.TripQueryService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerationService.class);

    private final TripQueryService tripQueryService;
    private final RecommendationPipelineService recommendationPipelineService;
    private final SchedulingInputMapper schedulingInputMapper;
    private final ItineraryScheduler itineraryScheduler;
    private final AiItineraryExplanationClient itineraryExplanationClient;

    public ItineraryGenerationService(
            TripQueryService tripQueryService,
            RecommendationPipelineService recommendationPipelineService,
            SchedulingInputMapper schedulingInputMapper,
            ItineraryScheduler itineraryScheduler,
            AiItineraryExplanationClient itineraryExplanationClient) {

        this.tripQueryService = tripQueryService;
        this.recommendationPipelineService = recommendationPipelineService;
        this.schedulingInputMapper = schedulingInputMapper;
        this.itineraryScheduler = itineraryScheduler;
        this.itineraryExplanationClient = itineraryExplanationClient;
    }

    @Transactional(readOnly = true)
    public ItineraryPlan generatePlan(Long userId, UUID tripPublicId, String preferenceDescription) {

        Trip trip = tripQueryService.findOwnedTrip(userId, tripPublicId);

        List<RecommendationCandidate> candidates = recommendationPipelineService.recommend(trip, preferenceDescription);
        PlanningContext context = schedulingInputMapper.toPlanningContext(trip);
        List<SchedulingCandidate> schedulingCandidates =
                schedulingInputMapper.toSchedulingCandidates(trip, candidates);

        return itineraryScheduler.schedule(context, schedulingCandidates);
    }

    @Transactional(readOnly = true)
    public GeneratedItineraryPreview generatePreview(Long userId, UUID tripPublicId, String preferenceDescription) {
        ItineraryPlan plan = generatePlan(userId, tripPublicId, preferenceDescription);
        List<String> selectedPlaceSlugs =
                plan.stops().stream().map(stop -> stop.place().slug()).toList();

        if (selectedPlaceSlugs.isEmpty()) {
            return new GeneratedItineraryPreview(plan, Map.of());
        }

        try {
            Map<String, String> reasons =
                    itineraryExplanationClient.generateReasons(preferenceDescription, selectedPlaceSlugs);
            return new GeneratedItineraryPreview(plan, reasons);
        } catch (RuntimeException exception) {
            log.warn("Itinerary explanation enrichment failed; returning preview without reasons", exception);
            return new GeneratedItineraryPreview(plan, Map.of());
        }
    }
}
