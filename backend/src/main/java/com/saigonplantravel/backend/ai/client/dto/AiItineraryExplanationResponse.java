package com.saigonplantravel.backend.ai.client.dto;

import java.util.List;

public record AiItineraryExplanationResponse(List<AiItineraryReasonResponse> reasons) {

    public AiItineraryExplanationResponse {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
