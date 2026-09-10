package com.saigonplantravel.backend.assistant.controller;

import com.saigonplantravel.backend.assistant.dto.AssistantMessageRequest;
import com.saigonplantravel.backend.assistant.dto.AssistantMessageResponse;
import com.saigonplantravel.backend.assistant.service.TripAssistantService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripPublicId}/assistant")
public class TripAssistantController {

    private final TripAssistantService tripAssistantService;

    public TripAssistantController(TripAssistantService tripAssistantService) {
        this.tripAssistantService = tripAssistantService;
    }

    @PostMapping("/messages")
    public AssistantMessageResponse sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tripPublicId,
            @Valid @RequestBody AssistantMessageRequest request) {
        return tripAssistantService.sendMessage(principal.id(), tripPublicId, request);
    }
}
