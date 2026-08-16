package com.saigonplantravel.backend.ai.client;

import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationRequest;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiRecommendationClient {
    private final RestClient restClient;

    public AiRecommendationClient(RestClient.Builder restClientBuilder, @Value("${app.ai.base-url}") String baseUrl) {

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public AiPlaceRecommendationResponse recommendPlaces(String query, int topK) {

        AiPlaceRecommendationRequest request = new AiPlaceRecommendationRequest(query, topK);

        AiPlaceRecommendationResponse response = restClient
                .post()
                .uri("/api/v1/recommendations/places")
                .body(request)
                .retrieve()
                .body(AiPlaceRecommendationResponse.class);

        if (response == null) {
            return new AiPlaceRecommendationResponse(null);
        }

        return response;
    }
}
