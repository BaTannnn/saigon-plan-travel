package com.saigonplantravel.backend.ai.client;

import com.saigonplantravel.backend.ai.client.dto.AiAssistantRequest;
import com.saigonplantravel.backend.ai.client.dto.AiAssistantResponse;
import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiAssistantClient {

    private final RestClient restClient;
    @Autowired
    public AiAssistantClient(RestClient.Builder restClientBuilder, @Value("${app.ai.base-url}") String baseUrl) {
        HttpClient httpClient =
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    AiAssistantClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AiAssistantResponse sendMessage(AiAssistantRequest request) {
        AiAssistantResponse response = restClient
                .post()
                .uri("/api/v1/assistant/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiAssistantResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI assistant returned an empty response");
        }
        return response;
    }
}
