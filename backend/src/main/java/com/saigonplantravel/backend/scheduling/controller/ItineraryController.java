package com.saigonplantravel.backend.scheduling.controller;

import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.scheduling.dto.ItineraryResponse;
import com.saigonplantravel.backend.scheduling.service.SchedulingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ItineraryController {

    private final SchedulingService schedulingService;

    public ItineraryController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @PostMapping("/trips/{tripPublicId}/itineraries")
    public ResponseEntity<ItineraryResponse> generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId
    ) {
        ItineraryResponse response = schedulingService.generate(
                principal.id(),
                tripPublicId
        );
        URI location = URI.create(
                "/api/v1/itineraries/" + response.publicId()
        );
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/itineraries/{itineraryPublicId}")
    public ItineraryResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itineraryPublicId
    ) {
        return schedulingService.get(
                principal.id(),
                itineraryPublicId
        );
    }
}
