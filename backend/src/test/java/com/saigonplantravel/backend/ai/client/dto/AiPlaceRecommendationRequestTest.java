package com.saigonplantravel.backend.ai.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiPlaceRecommendationRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesOnlyQueryAndTopKUsingPythonContractNames() throws Exception {
        AiPlaceRecommendationRequest request = new AiPlaceRecommendationRequest("Tôi thích lịch sử và kiến trúc", 30);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.get("query").asText()).isEqualTo("Tôi thích lịch sử và kiến trúc");
        assertThat(json.get("top_k").asInt()).isEqualTo(30);
        assertThat(json.size()).isEqualTo(2);
    }
}
