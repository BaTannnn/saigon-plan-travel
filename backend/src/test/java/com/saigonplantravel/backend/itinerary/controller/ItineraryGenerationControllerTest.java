package com.saigonplantravel.backend.itinerary.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.itinerary.dto.ApplyGeneratedItineraryRequest;
import com.saigonplantravel.backend.itinerary.dto.GeneratedItineraryStopResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailItemResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryGenerationPreviewResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryPlaceResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryScheduleResponse;
import com.saigonplantravel.backend.itinerary.dto.ItinerarySummaryResponse;
import com.saigonplantravel.backend.itinerary.exception.InvalidGeneratedItineraryException;
import com.saigonplantravel.backend.itinerary.mapper.ItineraryGenerationPreviewMapper;
import com.saigonplantravel.backend.itinerary.service.ItineraryGenerationService;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import java.math.BigDecimal;
import java.time.LocalTime;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ItineraryGenerationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, RestAuthenticationEntryPoint.class})
class ItineraryGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItineraryGenerationService itineraryGenerationService;

    @MockitoBean
    private ItineraryGenerationPreviewMapper previewMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @MockitoBean
    private ItineraryService itineraryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectsUnauthenticatedGenerationPreview() throws Exception {

        UUID tripPublicId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/trips/{tripPublicId}/itinerary/generation-preview", tripPublicId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "preferenceDescription":
                                            "Tôi thích lịch sử"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(itineraryGenerationService, previewMapper);
    }

    @Test
    void generatesPreviewUsingAuthenticatedUserAndPreference() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        String preference = "Tôi thích lịch sử, kiến trúc và muốn tiết kiệm chi phí";

        ItineraryPlan plan = new ItineraryPlan(List.of(), BigDecimal.ZERO, 0, 0, 0.0);

        ItineraryGenerationPreviewResponse response = previewResponse();

        when(itineraryGenerationService.generatePlan(principal.id(), tripPublicId, preference))
                .thenReturn(plan);

        when(previewMapper.toResponse(plan)).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/trips/{tripPublicId}/itinerary/generation-preview", tripPublicId)
                                .with(authenticatedAs(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "preferenceDescription":
                                            "Tôi thích lịch sử, kiến trúc và muốn tiết kiệm chi phí"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stops").isArray())
                .andExpect(jsonPath("$.stops[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.stops[0].place.slug").value("dinh-doc-lap"))
                .andExpect(jsonPath("$.stops[0].place.name").value("Dinh Độc Lập"))
                .andExpect(jsonPath("$.stops[0].place.latitude").value(10.7769))
                .andExpect(jsonPath("$.stops[0].place.longitude").value(106.6953))
                .andExpect(jsonPath("$.stops[0].schedule.arrivalTime").value("08:15:00"))
                .andExpect(jsonPath("$.stops[0].schedule.travelMinutes").value(15))
                .andExpect(jsonPath("$.summary.totalEstimatedCost").value(40000))
                .andExpect(jsonPath("$.summary.totalTravelMinutes").value(15))
                .andExpect(jsonPath("$.summary.totalVisitMinutes").value(90))
                .andExpect(jsonPath("$.summary.totalDistanceKm").value(3.2))
                .andExpect(jsonPath("$.issues").isEmpty());

        verify(itineraryGenerationService).generatePlan(principal.id(), tripPublicId, preference);

        verify(previewMapper).toResponse(plan);
    }

    @Test
    void rejectsBlankPreferenceBeforeCallingGenerationService() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/trips/{tripPublicId}/itinerary/generation-preview", tripPublicId)
                                .with(authenticatedAs(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "preferenceDescription": ""
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(itineraryGenerationService, previewMapper);
    }

    @Test
    void appliesGeneratedItineraryUsingApprovedPlaceOrder() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        List<String> placeSlugs = List.of("dinh-doc-lap", "bao-tang-my-thuat");

        ApplyGeneratedItineraryRequest request = new ApplyGeneratedItineraryRequest(placeSlugs);

        ItineraryDetailResponse response = appliedItineraryResponse(tripPublicId);

        when(itineraryService.applyGeneratedItinerary(principal.id(), tripPublicId, placeSlugs))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/itinerary/generation-apply", tripPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.tripPublicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))

                // Stop 1
                .andExpect(jsonPath("$.items[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.items[0].place.slug").value("dinh-doc-lap"))
                .andExpect(jsonPath("$.items[0].place.name").value("Dinh Độc Lập"))
                .andExpect(jsonPath("$.items[0].place.latitude").value(10.7769))
                .andExpect(jsonPath("$.items[0].place.longitude").value(106.6953))
                .andExpect(jsonPath("$.items[0].schedule").exists())

                // Stop 2
                .andExpect(jsonPath("$.items[1].sequenceNo").value(2))
                .andExpect(jsonPath("$.items[1].place.slug").value("bao-tang-my-thuat"))
                .andExpect(jsonPath("$.items[1].place.name").value("Bảo tàng Mỹ thuật"))
                .andExpect(jsonPath("$.items[1].schedule").exists())

                // Summary
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.totalEstimatedCost").value(70000))
                .andExpect(jsonPath("$.summary.totalTravelMinutes").value(25))
                .andExpect(jsonPath("$.summary.totalVisitMinutes").value(180))
                .andExpect(jsonPath("$.summary.totalDistanceKm").value(5.0))

                // Issues
                .andExpect(jsonPath("$.issues").isArray())
                .andExpect(jsonPath("$.issues").isEmpty());

        verify(itineraryService).applyGeneratedItinerary(principal.id(), tripPublicId, placeSlugs);
    }

    @Test
    void rejectsEmptyGeneratedItineraryBeforeCallingService() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/trips/{tripPublicId}/itinerary/generation-apply", tripPublicId)
                                .with(authenticatedAs(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "placeSlugs": []
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(itineraryService);
    }

    @Test
    void mapsInvalidGeneratedItineraryToBadRequest() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        List<String> placeSlugs = List.of("invalid-place");

        ApplyGeneratedItineraryRequest request = new ApplyGeneratedItineraryRequest(placeSlugs);

        when(itineraryService.applyGeneratedItinerary(principal.id(), tripPublicId, placeSlugs))
                .thenThrow(new InvalidGeneratedItineraryException());

        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/itinerary/generation-apply", tripPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_GENERATED_ITINERARY"))
                .andExpect(jsonPath("$.title").value("Invalid generated itinerary"));

        verify(itineraryService).applyGeneratedItinerary(principal.id(), tripPublicId, placeSlugs);
    }

    private ItineraryGenerationPreviewResponse previewResponse() {

        ItineraryPlaceResponse place = new ItineraryPlaceResponse(
                "dinh-doc-lap", "Dinh Độc Lập", new BigDecimal("10.7769"), new BigDecimal("106.6953"));

        ItineraryScheduleResponse schedule = new ItineraryScheduleResponse(
                LocalTime.of(8, 15), LocalTime.of(8, 15), LocalTime.of(9, 45), 15, 3.2, new BigDecimal("40000"));

        GeneratedItineraryStopResponse stop = new GeneratedItineraryStopResponse(1, place, schedule);

        ItinerarySummaryResponse summary = new ItinerarySummaryResponse(new BigDecimal("40000"), 15, 90, 3.2);

        return new ItineraryGenerationPreviewResponse(List.of(stop), summary, List.of());
    }

    private RequestPostProcessor authenticatedAs(UserPrincipal principal) {

        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }

    private UserPrincipal userPrincipal() {

        return new UserPrincipal(99L, UUID.randomUUID(), "tan@example.com", "Nguyễn Bá Tân", UserRole.USER);
    }

    private ItineraryDetailResponse appliedItineraryResponse(UUID tripPublicId) {

        ItineraryPlaceResponse firstPlace = new ItineraryPlaceResponse(
                "dinh-doc-lap", "Dinh Độc Lập", new BigDecimal("10.7769"), new BigDecimal("106.6953"));

        ItineraryScheduleResponse firstSchedule = new ItineraryScheduleResponse(
                LocalTime.of(8, 15), LocalTime.of(8, 15), LocalTime.of(9, 45), 15, 3.2, new BigDecimal("40000"));

        ItineraryDetailItemResponse firstItem =
                new ItineraryDetailItemResponse(UUID.randomUUID(), 1, firstPlace, firstSchedule);

        ItineraryPlaceResponse secondPlace = new ItineraryPlaceResponse(
                "bao-tang-my-thuat", "Bảo tàng Mỹ thuật", new BigDecimal("10.7698"), new BigDecimal("106.6994"));

        ItineraryScheduleResponse secondSchedule = new ItineraryScheduleResponse(
                LocalTime.of(9, 55), LocalTime.of(9, 55), LocalTime.of(11, 25), 10, 1.8, new BigDecimal("30000"));

        ItineraryDetailItemResponse secondItem =
                new ItineraryDetailItemResponse(UUID.randomUUID(), 2, secondPlace, secondSchedule);

        ItinerarySummaryResponse summary = new ItinerarySummaryResponse(new BigDecimal("70000"), 25, 180, 5.0);

        return new ItineraryDetailResponse(
                UUID.randomUUID(), tripPublicId, List.of(firstItem, secondItem), summary, List.of());
    }
}
