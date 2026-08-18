package com.saigonplantravel.backend.itinerary.controller;

import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.itinerary.dto.ApplyGeneratedItineraryRequest;
import com.saigonplantravel.backend.itinerary.dto.GenerateItineraryPreviewRequest;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryGenerationPreviewResponse;
import com.saigonplantravel.backend.itinerary.mapper.ItineraryGenerationPreviewMapper;
import com.saigonplantravel.backend.itinerary.service.ItineraryGenerationService;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripPublicId}/itinerary")
public class ItineraryGenerationController {

    private final ItineraryGenerationService itineraryGenerationService;
    private final ItineraryGenerationPreviewMapper previewMapper;
    private final ItineraryService itineraryService;

    public ItineraryGenerationController(
            ItineraryGenerationService itineraryGenerationService,
            ItineraryGenerationPreviewMapper previewMapper,
            ItineraryService itineraryService) {

        this.itineraryGenerationService = itineraryGenerationService;

        this.previewMapper = previewMapper;

        this.itineraryService = itineraryService;
    }

    @PostMapping("/generation-preview")
    public ItineraryGenerationPreviewResponse generatePreview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @Valid @RequestBody GenerateItineraryPreviewRequest request) {

        ItineraryPlan plan =
                itineraryGenerationService.generatePlan(principal.id(), tripPublicId, request.preferenceDescription());

        return previewMapper.toResponse(plan);
    }

    @PostMapping("/generation-apply")
    public ItineraryDetailResponse applyGeneratedItinerary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @Valid @RequestBody ApplyGeneratedItineraryRequest request) {

        return itineraryService.applyGeneratedItinerary(principal.id(), tripPublicId, request.placeSlugs());
    }
}
