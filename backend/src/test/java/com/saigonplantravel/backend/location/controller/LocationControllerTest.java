package com.saigonplantravel.backend.location.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import com.saigonplantravel.backend.location.exception.GeocodingUnavailableException;
import com.saigonplantravel.backend.location.service.LocationSearchService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(LocationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, RestAuthenticationEntryPoint.class})
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationSearchService locationSearchService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @Test
    void returnsAuthenticatedOwnedLocationContract() throws Exception {
        when(locationSearchService.search("  Dinh Độc Lập  "))
                .thenReturn(List.of(new LocationSearchResponse(
                        "Dinh Độc Lập, Quận 1", new BigDecimal("10.7770754"), new BigDecimal("106.6952941"))));

        mockMvc.perform(get("/api/v1/locations/search")
                        .param("q", "  Dinh Độc Lập  ")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0]", aMapWithSize(3)))
                .andExpect(jsonPath("$[0].label").value("Dinh Độc Lập, Quận 1"))
                .andExpect(jsonPath("$[0].latitude").value(10.7770754))
                .andExpect(jsonPath("$[0].longitude").value(106.6952941))
                .andExpect(jsonPath("$[0].placeId").doesNotExist());

        verify(locationSearchService).search("  Dinh Độc Lập  ");
    }

    @Test
    void rejectsBlankAndOversizedQueriesBeforeCallingProvider() throws Exception {
        mockMvc.perform(get("/api/v1/locations/search").param("q", "   ").with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/locations/search")
                        .param("q", "x".repeat(121))
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(locationSearchService);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/locations/search").param("q", "Dinh Độc Lập"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(locationSearchService);
    }

    @Test
    void mapsProviderFailureToBadGatewayWithoutLeakingDetails() throws Exception {
        when(locationSearchService.search("Dinh Độc Lập"))
                .thenThrow(new GeocodingUnavailableException("secret provider response"));

        mockMvc.perform(get("/api/v1/locations/search")
                        .param("q", "Dinh Độc Lập")
                        .with(authenticatedUser()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GEOCODING_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Location search is temporarily unavailable"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret"))));
    }

    private RequestPostProcessor authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(
                99L,
                UUID.fromString("8f047237-a9fb-4be0-becb-3cffaf445acd"),
                "tan@example.com",
                "Nguyễn Bá Tân",
                UserRole.USER);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }
}
