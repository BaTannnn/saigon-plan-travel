package com.saigonplantravel.backend.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.saigonplantravel.backend.recommendation.model.SemanticPlaceCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiRecommendationClientTest {

    @Test
    void mapsTransportCandidatesToSemanticCandidatesInResponseOrder() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiRecommendationClient client = new AiRecommendationClient(builder.build());

        server.expect(requestTo("https://ai.test/api/v1/recommendations/places"))
                .andExpect(
                        content()
                                .json(
                                        """
                                {
                                  "query": "architecture",
                                  "top_k": 30
                                }
                                """))
                .andRespond(withSuccess(
                        """
                        {
                          "candidates": [
                            {
                              "place_slug": "place-b",
                              "place_name": "Transport-only name B",
                              "matched_section": "history",
                              "semantic_score": 0.94
                            },
                            {
                              "place_slug": "place-a",
                              "place_name": "Transport-only name A",
                              "matched_section": "architecture",
                              "semantic_score": 0.89
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<SemanticPlaceCandidate> result = client.retrieve("architecture", 30);

        assertThat(result)
                .containsExactly(
                        new SemanticPlaceCandidate("place-b", 0.94, "history"),
                        new SemanticPlaceCandidate("place-a", 0.89, "architecture"));
        server.verify();
    }

    @Test
    void returnsEmptyCandidatesWhenTransportBodyIsEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiRecommendationClient client = new AiRecommendationClient(builder.build());

        server.expect(requestTo("https://ai.test/api/v1/recommendations/places"))
                .andRespond(withSuccess());

        assertThat(client.retrieve("architecture", 30)).isEmpty();
        server.verify();
    }
}
