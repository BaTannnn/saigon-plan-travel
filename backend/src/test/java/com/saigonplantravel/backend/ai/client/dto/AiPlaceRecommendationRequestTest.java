package com.saigonplantravel.backend.ai.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPlaceRecommendationRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesEligibilityWhitelistUsingPythonContractNames() throws Exception {
        AiPlaceRecommendationRequest request = new AiPlaceRecommendationRequest(
                "Tôi thích lịch sử và kiến trúc", 15, List.of("dinh-doc-lap", "buu-dien-trung-tam-sai-gon"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.get("query").asText()).isEqualTo("Tôi thích lịch sử và kiến trúc");
        assertThat(json.get("top_k").asInt()).isEqualTo(15);
        assertThat(json.get("eligible_place_slugs").get(0).asText()).isEqualTo("dinh-doc-lap");
        assertThat(json.get("eligible_place_slugs").get(1).asText()).isEqualTo("buu-dien-trung-tam-sai-gon");
    }
}
