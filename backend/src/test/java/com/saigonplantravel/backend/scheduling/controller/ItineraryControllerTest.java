package com.saigonplantravel.backend.scheduling.controller;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;
import com.saigonplantravel.backend.scheduling.domain.ItineraryWarningCode;
import com.saigonplantravel.backend.scheduling.dto.ItineraryResponse;
import com.saigonplantravel.backend.scheduling.dto.RejectionSummary;
import com.saigonplantravel.backend.scheduling.exception.ItineraryNotFoundException;
import com.saigonplantravel.backend.scheduling.exception.NoFeasibleItineraryException;
import com.saigonplantravel.backend.scheduling.exception.SchedulingDataConflictException;
import com.saigonplantravel.backend.scheduling.service.SchedulingService;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItineraryController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class
})
class ItineraryControllerTest {

    private static final UUID TRIP_PUBLIC_ID = UUID.fromString(
            "7a674ef0-57c8-4d0e-b99b-dccfd342fc98"
    );
    private static final UUID ITINERARY_PUBLIC_ID = UUID.fromString(
            "76aab24a-1938-449f-a12c-0cd273710afa"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulingService schedulingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @Test
    void generatesItineraryWithLocationAndPublicSnapshot() throws Exception {
        UserPrincipal principal = userPrincipal();
        when(schedulingService.generate(principal.id(), TRIP_PUBLIC_ID))
                .thenReturn(response(false));

        mockMvc.perform(
                        post(
                                "/api/v1/trips/{tripPublicId}/itineraries",
                                TRIP_PUBLIC_ID
                        ).with(authenticatedAs(principal))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "/api/v1/itineraries/" + ITINERARY_PUBLIC_ID
                ))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(12)))
                .andExpect(jsonPath("$.publicId").value(ITINERARY_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.tripPublicId").value(TRIP_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.algorithmVersion").value("GREEDY_V1"))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.assumptions.travelEstimator").value("HAVERSINE"))
                .andExpect(jsonPath("$.items[0].sequence").value(1))
                .andExpect(jsonPath("$.items[0].place.id").value(21))
                .andExpect(jsonPath("$.items[0].place.administrativeUnitName").isEmpty())
                .andExpect(jsonPath("$.items[0].travel.estimatedMinutes").value(7))
                .andExpect(jsonPath("$.summary.scheduledCount").value(1))
                .andExpect(jsonPath("$.warnings[0].code")
                        .value("TRAVEL_TIME_ESTIMATED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.tripId").doesNotExist());

        verify(schedulingService).generate(principal.id(), TRIP_PUBLIC_ID);
    }

    @Test
    void getsStoredItineraryAndReturnsStaleFlag() throws Exception {
        UserPrincipal principal = userPrincipal();
        when(schedulingService.get(principal.id(), ITINERARY_PUBLIC_ID))
                .thenReturn(response(true));

        mockMvc.perform(
                        get(
                                "/api/v1/itineraries/{itineraryPublicId}",
                                ITINERARY_PUBLIC_ID
                        ).with(authenticatedAs(principal))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(ITINERARY_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.stale").value(true));
    }

    @Test
    void mapsNoFeasibleResultToProblemDetail() throws Exception {
        UserPrincipal principal = userPrincipal();
        when(schedulingService.generate(principal.id(), TRIP_PUBLIC_ID))
                .thenThrow(new NoFeasibleItineraryException(
                        new RejectionSummary(3, 1, 1, 0, 1, 0)
                ));

        mockMvc.perform(
                        post(
                                "/api/v1/trips/{tripPublicId}/itineraries",
                                TRIP_PUBLIC_ID
                        ).with(authenticatedAs(principal))
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NO_FEASIBLE_ITINERARY"))
                .andExpect(jsonPath("$.rejectionSummary.candidatePoolSize").value(3))
                .andExpect(jsonPath("$.rejectionSummary.rejectedByEnvironment").value(1))
                .andExpect(jsonPath("$.rejectionSummary.rejectedByTripWindow").value(1));
    }

    @Test
    void mapsMissingOwnedItineraryToNotFound() throws Exception {
        UserPrincipal principal = userPrincipal();
        when(schedulingService.get(principal.id(), ITINERARY_PUBLIC_ID))
                .thenThrow(new ItineraryNotFoundException());

        mockMvc.perform(
                        get(
                                "/api/v1/itineraries/{itineraryPublicId}",
                                ITINERARY_PUBLIC_ID
                        ).with(authenticatedAs(principal))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITINERARY_NOT_FOUND"));
    }

    @Test
    void mapsSchedulingDataConflictToProblemDetail() throws Exception {
        UserPrincipal principal = userPrincipal();
        when(schedulingService.generate(principal.id(), TRIP_PUBLIC_ID))
                .thenThrow(new SchedulingDataConflictException(
                        "Place scheduling snapshot is inconsistent"
                ));

        mockMvc.perform(
                        post(
                                "/api/v1/trips/{tripPublicId}/itineraries",
                                TRIP_PUBLIC_ID
                        ).with(authenticatedAs(principal))
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("SCHEDULING_DATA_CONFLICT"))
                .andExpect(jsonPath("$.detail")
                        .value("Place scheduling snapshot is inconsistent"));
    }

    @Test
    void rejectsMalformedUuidBeforeCallingService() throws Exception {
        mockMvc.perform(
                        get("/api/v1/itineraries/not-a-uuid")
                                .with(authenticatedAs(userPrincipal()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(schedulingService);
    }

    @Test
    void requiresAuthenticationForGenerateAndGet() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/trips/{tripPublicId}/itineraries",
                        TRIP_PUBLIC_ID
                ))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(
                        "/api/v1/itineraries/{itineraryPublicId}",
                        ITINERARY_PUBLIC_ID
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(schedulingService);
    }

    private ItineraryResponse response(boolean stale) {
        return new ItineraryResponse(
                ITINERARY_PUBLIC_ID,
                TRIP_PUBLIC_ID,
                "GREEDY_V1",
                OffsetDateTime.parse("2026-08-09T16:00:00+07:00"),
                stale,
                new ItineraryResponse.SchedulingAssumptions(
                        "HAVERSINE",
                        new BigDecimal("18.00"),
                        5,
                        "MIN_COST",
                        false
                ),
                new ItineraryResponse.TripWindow(
                        LocalDate.of(2026, 8, 20),
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0)
                ),
                new ItineraryResponse.PreferencesSnapshot(
                        TravelPace.BALANCED,
                        EnvironmentPreference.MIXED,
                        Set.of(1L, 3L)
                ),
                new ItineraryResponse.Coordinate(
                        new BigDecimal("10.7726400"),
                        new BigDecimal("106.6980500")
                ),
                List.of(new ItineraryResponse.Item(
                        1,
                        new ItineraryResponse.PlaceSnapshot(
                                21L,
                                "Địa điểm demo",
                                "dia-diem-demo",
                                "Địa chỉ demo, TP.HCM",
                                null,
                                null,
                                new BigDecimal("10.7768890"),
                                new BigDecimal("106.7008060"),
                                false
                        ),
                        new ItineraryResponse.TravelSnapshot(
                                7,
                                new BigDecimal("0.510")
                        ),
                        LocalTime.of(8, 7),
                        0,
                        LocalTime.of(8, 7),
                        LocalTime.of(9, 37),
                        90,
                        90,
                        new BigDecimal("0.00"),
                        new BigDecimal("90.8346"),
                        new ItineraryResponse.OpeningHours(
                                OpeningHoursStatus.UNKNOWN,
                                null,
                                null
                        )
                )),
                new ItineraryResponse.Summary(
                        3,
                        1,
                        new BigDecimal("0.510"),
                        new BigDecimal("0.00"),
                        new BigDecimal("500000.00"),
                        7,
                        90,
                        0,
                        503
                ),
                List.of(new ItineraryResponse.Warning(
                        ItineraryWarningCode.TRAVEL_TIME_ESTIMATED,
                        ItineraryWarningCode.TRAVEL_TIME_ESTIMATED.message(),
                        null
                ))
        );
    }

    private RequestPostProcessor authenticatedAs(UserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.authorities()
        ));
    }

    private UserPrincipal userPrincipal() {
        return new UserPrincipal(
                99L,
                UUID.fromString("8f047237-a9fb-4be0-becb-3cffaf445acd"),
                "tan@example.com",
                "Nguyễn Bá Tân",
                UserRole.USER
        );
    }
}
