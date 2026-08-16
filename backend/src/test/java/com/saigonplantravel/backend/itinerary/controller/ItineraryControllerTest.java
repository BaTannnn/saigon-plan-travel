package com.saigonplantravel.backend.itinerary.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailItemResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryPlaceResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryScheduleResponse;
import com.saigonplantravel.backend.itinerary.dto.ItinerarySummaryResponse;
import com.saigonplantravel.backend.itinerary.dto.SaveItineraryItemRequest;
import com.saigonplantravel.backend.itinerary.exception.DuplicateItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.ItineraryItemNotFoundException;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
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

@WebMvcTest(ItineraryController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, RestAuthenticationEntryPoint.class})
class ItineraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ItineraryService itineraryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {

        mockMvc.perform(get("/api/v1/trips/{tripPublicId}/itinerary", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(itineraryService);
    }

    @Test
    void getsEmptyItineraryUsingAuthenticatedUserId() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        ItineraryDetailResponse response = emptyItineraryResponse(tripPublicId);

        when(itineraryService.getItinerary(principal.id(), tripPublicId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trips/{tripPublicId}/itinerary", tripPublicId)
                        .with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripPublicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.summary.totalEstimatedCost").value(0))
                .andExpect(jsonPath("$.summary.totalTravelMinutes").value(0))
                .andExpect(jsonPath("$.summary.totalVisitMinutes").value(0))
                .andExpect(jsonPath("$.summary.totalDistanceKm").value(0.0))
                .andExpect(jsonPath("$.issues").isArray());

        verify(itineraryService).getItinerary(principal.id(), tripPublicId);
    }

    @Test
    void addsPlaceAndReturnsUpdatedCalculatedItinerary() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        SaveItineraryItemRequest request = new SaveItineraryItemRequest(42L);

        when(itineraryService.addItem(principal.id(), tripPublicId, request.placeId()))
                .thenReturn(itineraryResponse(tripPublicId));

        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/itinerary/items", tripPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripPublicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$.items[0].publicId").exists())
                .andExpect(jsonPath("$.items[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.items[0].place.slug").value("cho-ben-thanh"))
                .andExpect(jsonPath("$.items[0].place.name").value("Chợ Bến Thành"))
                /*
                 * Contract mới không expose
                 * internal database Place.id.
                 */
                .andExpect(jsonPath("$.items[0].place.id").doesNotExist())
                .andExpect(jsonPath("$.items[0].schedule.arrivalTime").value("08:10:00"))
                .andExpect(jsonPath("$.items[0].schedule.visitStartTime").value("08:10:00"))
                .andExpect(jsonPath("$.items[0].schedule.visitEndTime").value("09:40:00"))
                .andExpect(jsonPath("$.items[0].schedule.travelMinutes").value(10))
                .andExpect(jsonPath("$.items[0].schedule.travelDistanceKm").value(1.5))
                .andExpect(jsonPath("$.items[0].schedule.estimatedCost").value(30000))
                .andExpect(jsonPath("$.summary.totalEstimatedCost").value(30000))
                .andExpect(jsonPath("$.summary.totalTravelMinutes").value(10))
                .andExpect(jsonPath("$.summary.totalVisitMinutes").value(90))
                .andExpect(jsonPath("$.summary.totalDistanceKm").value(1.5))
                .andExpect(jsonPath("$.issues").isEmpty());

        verify(itineraryService).addItem(principal.id(), tripPublicId, request.placeId());
    }

    @Test
    void deletesItemUsingTripAndItemPublicIds() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        UUID itemPublicId = UUID.randomUUID();

        when(itineraryService.deleteItem(principal.id(), tripPublicId, itemPublicId))
                .thenReturn(new ItineraryDetailResponse(
                        UUID.randomUUID(), tripPublicId, List.of(), emptySummary(), List.of()));

        mockMvc.perform(delete(
                                "/api/v1/trips/{tripPublicId}/itinerary/items/{itemPublicId}",
                                tripPublicId,
                                itemPublicId)
                        .with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.summary.totalEstimatedCost").value(0))
                .andExpect(jsonPath("$.summary.totalTravelMinutes").value(0))
                .andExpect(jsonPath("$.issues").isEmpty());

        verify(itineraryService).deleteItem(principal.id(), tripPublicId, itemPublicId);
    }

    @Test
    void replacesItemPlaceUsingTripAndItemPublicIds() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        UUID itemPublicId = UUID.randomUUID();

        SaveItineraryItemRequest request = new SaveItineraryItemRequest(42L);

        when(itineraryService.replaceItemPlace(principal.id(), tripPublicId, itemPublicId, request.placeId()))
                .thenReturn(itineraryResponse(tripPublicId));

        mockMvc.perform(put("/api/v1/trips/{tripPublicId}/itinerary/items/{itemPublicId}", tripPublicId, itemPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].place.slug").value("cho-ben-thanh"))
                .andExpect(jsonPath("$.items[0].place.id").doesNotExist())
                .andExpect(jsonPath("$.items[0].schedule").exists())
                .andExpect(jsonPath("$.summary").exists());

        verify(itineraryService).replaceItemPlace(principal.id(), tripPublicId, itemPublicId, request.placeId());
    }

    @Test
    void rejectsMissingPlaceIdBeforeCallingService() throws Exception {

        UserPrincipal principal = userPrincipal();

        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/itinerary/items", UUID.randomUUID())
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(itineraryService);
    }

    @Test
    void mapsDuplicateAndForeignItemFailuresToSafeStatuses() throws Exception {

        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.randomUUID();

        UUID itemPublicId = UUID.randomUUID();

        when(itineraryService.addItem(principal.id(), tripPublicId, 42L))
                .thenThrow(new DuplicateItineraryPlaceException());

        mockMvc.perform(post("/api/v1/trips/{tripPublicId}/itinerary/items", tripPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeId\":42}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ITINERARY_PLACE"));

        when(itineraryService.deleteItem(principal.id(), tripPublicId, itemPublicId))
                .thenThrow(new ItineraryItemNotFoundException());

        mockMvc.perform(delete(
                                "/api/v1/trips/{tripPublicId}/itinerary/items/{itemPublicId}",
                                tripPublicId,
                                itemPublicId)
                        .with(authenticatedAs(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITINERARY_ITEM_NOT_FOUND"));
    }

    private ItineraryDetailResponse itineraryResponse(UUID tripPublicId) {

        ItineraryPlaceResponse place = new ItineraryPlaceResponse("cho-ben-thanh", "Chợ Bến Thành");

        ItineraryScheduleResponse schedule = new ItineraryScheduleResponse(
                LocalTime.of(8, 10), LocalTime.of(8, 10), LocalTime.of(9, 40), 10, 1.5, new BigDecimal("30000"));

        ItineraryDetailItemResponse item = new ItineraryDetailItemResponse(UUID.randomUUID(), 1, place, schedule);

        ItinerarySummaryResponse summary = new ItinerarySummaryResponse(new BigDecimal("30000"), 10, 90, 1.5);

        return new ItineraryDetailResponse(UUID.randomUUID(), tripPublicId, List.of(item), summary, List.of());
    }

    private ItineraryDetailResponse emptyItineraryResponse(UUID tripPublicId) {

        return new ItineraryDetailResponse(null, tripPublicId, List.of(), emptySummary(), List.of());
    }

    private ItinerarySummaryResponse emptySummary() {

        return new ItinerarySummaryResponse(BigDecimal.ZERO, 0, 0, 0.0);
    }

    private RequestPostProcessor authenticatedAs(UserPrincipal principal) {

        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }

    private UserPrincipal userPrincipal() {

        return new UserPrincipal(99L, UUID.randomUUID(), "tan@example.com", "Nguyễn Bá Tân", UserRole.USER);
    }
}
