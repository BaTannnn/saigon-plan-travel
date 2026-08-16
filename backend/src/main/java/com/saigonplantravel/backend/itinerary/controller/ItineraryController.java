package com.saigonplantravel.backend.itinerary.controller;

import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.dto.ReorderItineraryItemsRequest;
import com.saigonplantravel.backend.itinerary.dto.SaveItineraryItemRequest;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripPublicId}/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping
    public ItineraryDetailResponse getItinerary(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID tripPublicId) {

        return itineraryService.getItinerary(principal.id(), tripPublicId);
    }

    @PostMapping("/items")
    public ItineraryDetailResponse addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @Valid @RequestBody SaveItineraryItemRequest request) {

        return itineraryService.addItem(principal.id(), tripPublicId, request.placeId());
    }

    @DeleteMapping("/items/{itemPublicId}")
    public ItineraryDetailResponse deleteItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @PathVariable UUID itemPublicId) {

        return itineraryService.deleteItem(principal.id(), tripPublicId, itemPublicId);
    }

    @PutMapping("/items/{itemPublicId}")
    public ItineraryDetailResponse replaceItemPlace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @PathVariable UUID itemPublicId,
            @Valid @RequestBody SaveItineraryItemRequest request) {

        return itineraryService.replaceItemPlace(principal.id(), tripPublicId, itemPublicId, request.placeId());
    }
    @PutMapping("/items/order")
    public ItineraryDetailResponse reorderItems(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @Valid @RequestBody ReorderItineraryItemsRequest request) {

        return itineraryService.reorderItems(
                principal.id(),
                tripPublicId,
                request.itemPublicIds());
    }
}
