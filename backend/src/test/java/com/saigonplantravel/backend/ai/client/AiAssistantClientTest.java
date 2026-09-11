package com.saigonplantravel.backend.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.saigonplantravel.backend.ai.client.dto.AiAssistantRequest;
import com.saigonplantravel.backend.ai.client.dto.AiAssistantResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiAssistantClientTest {

    @Test
    void mapsAssistantRequestAndStructuredResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiAssistantClient client = new AiAssistantClient(builder.build());
        AiAssistantRequest request = new AiAssistantRequest(
                "Tôi thích lịch sử",
                List.of(new AiAssistantRequest.ConversationMessage("USER", "Ưu tiên bảo tàng")),
                List.of("dinh-doc-lap"));

        server.expect(requestTo("https://ai.test/api/v1/assistant/messages"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        content()
                                .json(
                                        """
                                {
                                  "message": "Tôi thích lịch sử",
                                  "history": [{"role": "USER", "content": "Ưu tiên bảo tàng"}],
                                  "excluded_place_slugs": ["dinh-doc-lap"]
                                }
                                """))
                .andRespond(withSuccess(
                        """
                        {
                          "answer": "Bạn có thể cân nhắc bảo tàng.",
                          "suggested_places": [{
                            "slug": "bao-tang-lich-su-tphcm",
                            "reason": "Phù hợp sở thích lịch sử."
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        AiAssistantResponse response = client.sendMessage(request);

        assertThat(response.suggestedPlaces())
                .singleElement()
                .extracting(AiAssistantResponse.SuggestedPlace::slug)
                .isEqualTo("bao-tang-lich-su-tphcm");
        server.verify();
    }
}
