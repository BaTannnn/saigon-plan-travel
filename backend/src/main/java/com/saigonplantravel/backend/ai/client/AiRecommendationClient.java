package com.saigonplantravel.backend.ai.client;

import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationRequest;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationResponse;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiRecommendationClient {
    private final RestClient restClient;

    public AiRecommendationClient(RestClient.Builder restClientBuilder, @Value("${app.ai.base-url}") String baseUrl) {
        HttpClient httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AiPlaceRecommendationResponse recommendPlaces(String query, int topK, List<String> eligiblePlaceSlugs) {

        AiPlaceRecommendationRequest request = new AiPlaceRecommendationRequest(query, topK, eligiblePlaceSlugs);

        AiPlaceRecommendationResponse response = restClient
                .post()
                .uri("/api/v1/recommendations/places")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiPlaceRecommendationResponse.class);

        if (response == null) {
            return new AiPlaceRecommendationResponse(null);
        }

        return response;
    }
}
