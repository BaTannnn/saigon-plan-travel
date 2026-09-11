package com.saigonplantravel.backend.assistant.service;

import com.saigonplantravel.backend.ai.client.AiAssistantClient;
import com.saigonplantravel.backend.ai.client.dto.AiAssistantRequest;
import com.saigonplantravel.backend.ai.client.dto.AiAssistantResponse;
import com.saigonplantravel.backend.assistant.dto.AssistantMessageRequest;
import com.saigonplantravel.backend.assistant.dto.AssistantMessageResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceQueryService;
import com.saigonplantravel.backend.trip.service.TripQueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TripAssistantService {

    private final TripQueryService tripQueryService;
    private final ItineraryService itineraryService;
    private final AiAssistantClient aiAssistantClient;
    private final PlaceQueryService placeQueryService;

    public TripAssistantService(
            TripQueryService tripQueryService,
            ItineraryService itineraryService,
            AiAssistantClient aiAssistantClient,
            PlaceQueryService placeQueryService) {
        this.tripQueryService = tripQueryService;
        this.itineraryService = itineraryService;
        this.aiAssistantClient = aiAssistantClient;
        this.placeQueryService = placeQueryService;
    }

    public AssistantMessageResponse sendMessage(Long userId, UUID tripPublicId, AssistantMessageRequest request) {
        tripQueryService.findOwnedTrip(userId, tripPublicId);
        ItineraryDetailResponse itinerary = itineraryService.getItinerary(userId, tripPublicId);
        Set<String> excludedPlaceSlugs = itinerary.items().stream()
                .map(item -> item.place().slug())
                .collect(Collectors.toSet());

        AiAssistantRequest aiRequest = toAiRequest(request, excludedPlaceSlugs);
        AiAssistantResponse aiResponse = aiAssistantClient.sendMessage(aiRequest);

        return new AssistantMessageResponse(
                aiResponse.answer(),
                validateRecommendations(aiResponse.suggestedPlaces(), excludedPlaceSlugs));
    }

    private AiAssistantRequest toAiRequest(AssistantMessageRequest request, Set<String> excludedPlaceSlugs) {
        List<AiAssistantRequest.ConversationMessage> history = request.history().stream()
                .map(item -> new AiAssistantRequest.ConversationMessage(
                        item.role().name(), item.content().trim()))
                .toList();

        return new AiAssistantRequest(
                request.message().trim(), history, excludedPlaceSlugs.stream().sorted().toList());
    }

    private List<AssistantMessageResponse.SuggestedPlace> validateRecommendations(
            List<AiAssistantResponse.SuggestedPlace> candidates, Set<String> excludedPlaceSlugs) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, String> reasonsBySlug = new LinkedHashMap<>();
        for (AiAssistantResponse.SuggestedPlace candidate : candidates) {
            if (candidate == null
                    || candidate.slug() == null
                    || candidate.slug().isBlank()
                    || excludedPlaceSlugs.contains(candidate.slug().trim())
                    || candidate.reason() == null
                    || candidate.reason().isBlank()) {
                continue;
            }
            reasonsBySlug.putIfAbsent(
                    candidate.slug().trim(), candidate.reason().trim());
        }
        if (reasonsBySlug.isEmpty()) {
            return List.of();
        }

        List<Place> activePlaces = placeQueryService.findAllActiveBySlugsForScheduling(reasonsBySlug.keySet());
        Map<String, Place> activeBySlug = activePlaces.stream()
                .filter(place -> Boolean.TRUE.equals(place.getActive()))
                .collect(Collectors.toMap(Place::getSlug, Function.identity()));

        return reasonsBySlug.entrySet().stream()
                .filter(entry -> activeBySlug.containsKey(entry.getKey()))
                .map(entry -> {
                    Place place = activeBySlug.get(entry.getKey());
                    return new AssistantMessageResponse.SuggestedPlace(
                            place.getId(),
                            place.getSlug(),
                            place.getName(),
                            place.getPrimaryImageUrl(),
                            entry.getValue());
                })
                .toList();
    }

}
