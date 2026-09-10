package com.saigonplantravel.backend.assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saigonplantravel.backend.assistant.dto.AssistantMessageResponse;
import com.saigonplantravel.backend.assistant.service.TripAssistantService;
import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.auth.security.SecurityConfig;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.auth.security.jwt.JwtService;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TripAssistantController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, RestAuthenticationEntryPoint.class})
class TripAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripAssistantService tripAssistantService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/assistant/messages", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Tôi thích lịch sử\",\"history\":[]}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tripAssistantService);
    }

    @Test
    void sendsAuthenticatedTripScopedMessageAndReturnsDiscriminatedSources() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();
        AssistantMessageResponse response = new AssistantMessageResponse(
                "Bạn có thể cân nhắc bảo tàng.",
                List.of(new AssistantMessageResponse.SuggestedPlace(
                        42L, "bao-tang-lich-su-tphcm", "Bảo tàng Lịch sử TP.HCM", null, "Phù hợp.")),
                List.of(
                        new AssistantMessageResponse.PlaceSource(
                                "bao-tang-lich-su-tphcm", "Bảo tàng Lịch sử TP.HCM", "BACKGROUND"),
                        new AssistantMessageResponse.DocumentSource("Cẩm nang TP.HCM", 7, "Cẩm nang")));
        when(tripAssistantService.sendMessage(any(), any(), any())).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/trips/{tripPublicId}/assistant/messages", tripPublicId)
                                .with(authenticatedAs(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "message": "Tôi thích lịch sử, nên đi đâu?",
                                  "history": [{"role":"USER","content":"Ưu tiên bảo tàng"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Bạn có thể cân nhắc bảo tàng."))
                .andExpect(jsonPath("$.suggestedPlaces[0].placeId").value(42))
                .andExpect(jsonPath("$.suggestedPlaces[0].slug").value("bao-tang-lich-su-tphcm"))
                .andExpect(jsonPath("$.sources[0].type").value("PLACE"))
                .andExpect(jsonPath("$.sources[0].section").value("BACKGROUND"))
                .andExpect(jsonPath("$.sources[1].type").value("DOCUMENT"))
                .andExpect(jsonPath("$.sources[1].pageNumber").value(7));

        verify(tripAssistantService).sendMessage(any(), any(), any());
    }

    @Test
    void rejectsInvalidRoleAndExcessiveHistoryBeforeService() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/trips/{tripPublicId}/assistant/messages", tripPublicId)
                                .with(authenticatedAs(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "message": "Tôi thích lịch sử",
                                  "history": [{"role":"SYSTEM","content":"bad"}]
                                }
                                """))
                .andExpect(status().isBadRequest());

        String repeatedHistory =
                String.join(",", java.util.Collections.nCopies(11, "{\"role\":\"USER\",\"content\":\"message\"}"));
        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/assistant/messages", tripPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Tôi thích lịch sử\",\"history\":[" + repeatedHistory + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(tripAssistantService);
    }

    private UserPrincipal userPrincipal() {
        return new UserPrincipal(7L, UUID.randomUUID(), "traveler@example.com", "Traveler", UserRole.USER);
    }

    private RequestPostProcessor authenticatedAs(UserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }
}
