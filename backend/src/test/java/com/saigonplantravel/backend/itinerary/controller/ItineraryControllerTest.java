package com.saigonplantravel.backend.itinerary.controller;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.itinerary.dto.ItineraryItemResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryResponse;
import com.saigonplantravel.backend.itinerary.dto.SaveItineraryItemRequest;
import com.saigonplantravel.backend.itinerary.exception.DuplicateItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.ItineraryItemNotFoundException;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

@WebMvcTest(ItineraryController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class
})
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
        mockMvc.perform(get(
                        "/api/v1/trips/{tripPublicId}/itinerary",
                        UUID.randomUUID()
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(itineraryService);
    }

    @Test
    void getsEmptyItineraryUsingAuthenticatedUserId()
            throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();
        ItineraryResponse response = new ItineraryResponse(
                null,
                tripPublicId,
                List.of(),
                null,
                null
        );

        when(itineraryService.getItinerary(
                principal.id(),
                tripPublicId
        )).thenReturn(response);

        mockMvc.perform(get(
                        "/api/v1/trips/{tripPublicId}/itinerary",
                        tripPublicId
                ).with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripPublicId")
                        .value(tripPublicId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(itineraryService).getItinerary(
                principal.id(),
                tripPublicId
        );
    }

    @Test
    void addsPlaceAndReturnsUpdatedItinerary() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();
        SaveItineraryItemRequest request =
                new SaveItineraryItemRequest(42L);

        when(itineraryService.addItem(
                principal.id(),
                tripPublicId,
                request.placeId()
        )).thenReturn(itineraryResponse(tripPublicId));

        mockMvc.perform(post(
                        "/api/v1/trips/{tripPublicId}/itinerary/items",
                        tripPublicId
                ).with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sequenceNo")
                        .value(1))
                .andExpect(jsonPath("$.items[0].place.id")
                        .value(42));

        verify(itineraryService).addItem(
                principal.id(),
                tripPublicId,
                request.placeId()
        );
    }

    @Test
    void deletesItemUsingTripAndItemPublicIds() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();
        UUID itemPublicId = UUID.randomUUID();

        when(itineraryService.deleteItem(
                principal.id(),
                tripPublicId,
                itemPublicId
        )).thenReturn(new ItineraryResponse(
                UUID.randomUUID(),
                tripPublicId,
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));

        mockMvc.perform(delete(
                        "/api/v1/trips/{tripPublicId}/itinerary/items/{itemPublicId}",
                        tripPublicId,
                        itemPublicId
                ).with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(itineraryService).deleteItem(
                principal.id(),
                tripPublicId,
                itemPublicId
        );
    }

    @Test
    void replacesItemPlaceUsingTripAndItemPublicIds()
            throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();
        UUID itemPublicId = UUID.randomUUID();
        SaveItineraryItemRequest request =
                new SaveItineraryItemRequest(42L);

        when(itineraryService.replaceItemPlace(
                principal.id(),
                tripPublicId,
                itemPublicId,
                request.placeId()
        )).thenReturn(itineraryResponse(tripPublicId));

        mockMvc.perform(put(
                        "/api/v1/trips/{tripPublicId}/itinerary/items/{itemPublicId}",
                        tripPublicId,
                        itemPublicId
                ).with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].place.id")
                        .value(42));

        verify(itineraryService).replaceItemPlace(
                principal.id(),
                tripPublicId,
                itemPublicId,
                request.placeId()
        );
    }

    @Test
    void rejectsMissingPlaceIdBeforeCallingService()
            throws Exception {
        UserPrincipal principal = userPrincipal();

        mockMvc.perform(post(
                        "/api/v1/trips/{tripPublicId}/itinerary/items",
                        UUID.randomUUID()
                ).with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(itineraryService);
    }

    @Test
    void mapsDuplicateAndForeignItemFailuresToSafeStatuses()
            throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();
        UUID itemPublicId = UUID.randomUUID();

        when(itineraryService.addItem(
                principal.id(),
                tripPublicId,
                42L
        )).thenThrow(new DuplicateItineraryPlaceException());

        mockMvc.perform(post(
                        "/api/v1/trips/{tripPublicId}/itinerary/items",
                        tripPublicId
                ).with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeId\":42}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("DUPLICATE_ITINERARY_PLACE"));

        when(itineraryService.deleteItem(
                principal.id(),
                tripPublicId,
                itemPublicId
        )).thenThrow(new ItineraryItemNotFoundException());

        mockMvc.perform(delete(
                        "/api/v1/trips/{tripPublicId}/itinerary/items/{itemPublicId}",
                        tripPublicId,
                        itemPublicId
                ).with(authenticatedAs(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("ITINERARY_ITEM_NOT_FOUND"));
    }

    private ItineraryResponse itineraryResponse(UUID tripPublicId) {
        OffsetDateTime timestamp = OffsetDateTime.parse(
                "2026-08-10T18:00:00+07:00"
        );
        PlaceSummaryResponse place = new PlaceSummaryResponse(
                42L,
                "Chợ Bến Thành",
                "cho-ben-thanh",
                "A landmark market",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                90,
                new BigDecimal("0.00"),
                new BigDecimal("100000.00"),
                false
        );
        ItineraryItemResponse item = new ItineraryItemResponse(
                UUID.randomUUID(),
                1,
                place,
                timestamp,
                timestamp
        );

        return new ItineraryResponse(
                UUID.randomUUID(),
                tripPublicId,
                List.of(item),
                timestamp,
                timestamp
        );
    }

    private RequestPostProcessor authenticatedAs(
            UserPrincipal principal
    ) {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.authorities()
                )
        );
    }

    private UserPrincipal userPrincipal() {
        return new UserPrincipal(
                99L,
                UUID.randomUUID(),
                "tan@example.com",
                "Nguyễn Bá Tân",
                UserRole.USER
        );
    }
}
