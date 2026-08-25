package com.saigonplantravel.backend.ai.client;

import com.saigonplantravel.backend.ai.client.dto.AiItineraryExplanationRequest;
import com.saigonplantravel.backend.ai.client.dto.AiItineraryExplanationResponse;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiItineraryExplanationClient {

    private final RestClient restClient;

    public AiItineraryExplanationClient(
            RestClient.Builder restClientBuilder, @Value("${app.ai.base-url}") String baseUrl) {
        HttpClient httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Map<String, String> generateReasons(String preference, List<String> placeSlugs) {
        AiItineraryExplanationRequest request = new AiItineraryExplanationRequest(preference, placeSlugs);

        AiItineraryExplanationResponse response = restClient
                .post()
                .uri("/api/v1/rag/itinerary-reasons")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiItineraryExplanationResponse.class);

        if (response == null) {
            return Map.of();
        }

        Map<String, String> reasonsBySlug = new LinkedHashMap<>();
        response.reasons().forEach(item -> {
            if (item != null
                    && item.placeSlug() != null
                    && item.reason() != null
                    && !item.reason().isBlank()) {
                reasonsBySlug.putIfAbsent(item.placeSlug(), item.reason().trim());
            }
        });

        return Map.copyOf(reasonsBySlug);
    }
}
