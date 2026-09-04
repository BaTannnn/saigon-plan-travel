package com.saigonplantravel.backend.ai.client;

import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationRequest;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationResponse;
import com.saigonplantravel.backend.recommendation.SemanticPlaceRetriever;
import com.saigonplantravel.backend.recommendation.model.SemanticPlaceCandidate;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiRecommendationClient implements SemanticPlaceRetriever {
    private final RestClient restClient;

    @Autowired
    public AiRecommendationClient(RestClient.Builder restClientBuilder, @Value("${app.ai.base-url}") String baseUrl) {
        HttpClient httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    AiRecommendationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<SemanticPlaceCandidate> retrieve(String query, int topK) {

        AiPlaceRecommendationRequest request = new AiPlaceRecommendationRequest(query, topK);

        AiPlaceRecommendationResponse response = restClient
                .post()
                .uri("/api/v1/recommendations/places")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiPlaceRecommendationResponse.class);

        if (response == null) {
            return List.of();
        }

        return response.candidates().stream()
                .map(candidate -> new SemanticPlaceCandidate(
                        candidate.placeSlug(), candidate.semanticScore(), candidate.matchedSection()))
                .toList();
    }
}
