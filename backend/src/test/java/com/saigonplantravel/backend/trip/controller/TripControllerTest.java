package com.saigonplantravel.backend.trip.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationResponse;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.service.TripService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(TripController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, RestAuthenticationEntryPoint.class})
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @Test
    void createsTripAndReturnsLocationHeader() throws Exception {
        UserPrincipal principal = userPrincipal();
        SaveTripRequest request = validRequest();

        UUID tripPublicId = UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98");

        TripResponse response = tripResponse(tripPublicId);

        when(tripService.createTrip(principal.id(), request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/trips")
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/trips/" + tripPublicId))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(10)))
                .andExpect(jsonPath("$.publicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$.tripDate").value("2026-08-20"))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.endTime").value("18:00"))
                .andExpect(jsonPath("$.budget").value(500000))
                .andExpect(jsonPath("$.startLocation.label").value("Chợ Bến Thành, Quận 1"))
                .andExpect(jsonPath("$.startLocation.latitude").value(10.7726400))
                .andExpect(jsonPath("$.startLocation.longitude").value(106.6980500))
                .andExpect(jsonPath("$.travelPace").value("BALANCED"))
                .andExpect(jsonPath("$.environmentPreference").value("MIXED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());

        verify(tripService).createTrip(principal.id(), request);
    }

    @Test
    void getsTripUsingAuthenticatedUserId() throws Exception {
        UserPrincipal principal = userPrincipal();

        UUID tripPublicId = UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98");

        when(tripService.getTrip(principal.id(), tripPublicId)).thenReturn(tripResponse(tripPublicId));

        mockMvc.perform(get("/api/v1/trips/{publicId}", tripPublicId).with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$.startTime").value("08:00"));

        verify(tripService).getTrip(principal.id(), tripPublicId);
    }

    @Test
    void listsTripsUsingAuthenticatedUserId() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98");

        when(tripService.listTrips(principal.id())).thenReturn(List.of(tripSummaryResponse(tripPublicId)));

        mockMvc.perform(get("/api/v1/trips").with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]", aMapWithSize(9)))
                .andExpect(jsonPath("$[0].publicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$[0].startLocationLabel").value("Chợ Bến Thành, Quận 1"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].startLocation").doesNotExist());

        verify(tripService).listTrips(principal.id());
    }

    @Test
    void listsTripsForSelectedMonth() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98");

        when(tripService.listTrips(principal.id(), 2026, 8)).thenReturn(List.of(tripSummaryResponse(tripPublicId)));

        mockMvc.perform(get("/api/v1/trips")
                        .queryParam("year", "2026")
                        .queryParam("month", "8")
                        .with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicId").value(tripPublicId.toString()));

        verify(tripService).listTrips(principal.id(), 2026, 8);
    }

    @Test
    void rejectsInvalidMonthWithoutCallingService() throws Exception {
        UserPrincipal principal = userPrincipal();

        mockMvc.perform(get("/api/v1/trips")
                        .queryParam("year", "2026")
                        .queryParam("month", "13")
                        .with(authenticatedAs(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRIP_MONTH_FILTER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("month"));

        verifyNoInteractions(tripService);
    }

    @Test
    void rejectsYearOrMonthWhenSuppliedAloneWithoutCallingService() throws Exception {
        UserPrincipal principal = userPrincipal();

        mockMvc.perform(get("/api/v1/trips").queryParam("year", "2026").with(authenticatedAs(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRIP_MONTH_FILTER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("month"));

        mockMvc.perform(get("/api/v1/trips").queryParam("month", "8").with(authenticatedAs(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRIP_MONTH_FILTER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("year"));

        verifyNoInteractions(tripService);
    }

    @Test
    void replacesTripUsingAuthenticatedUserId() throws Exception {

        UserPrincipal principal = userPrincipal();
        SaveTripRequest request = validRequest();

        UUID tripPublicId = UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98");

        when(tripService.replaceTrip(principal.id(), tripPublicId, request)).thenReturn(tripResponse(tripPublicId));

        mockMvc.perform(put("/api/v1/trips/{publicId}", tripPublicId)
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(tripPublicId.toString()))
                .andExpect(jsonPath("$.tripDate").value("2026-08-20"));

        verify(tripService).replaceTrip(principal.id(), tripPublicId, request);
    }

    @Test
    void deletesTripUsingAuthenticatedUserIdAndReturnsNoContent() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.fromString("7a674ef0-57c8-4d0e-b99b-dccfd342fc98");

        mockMvc.perform(delete("/api/v1/trips/{publicId}", tripPublicId).with(authenticatedAs(principal)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tripService).deleteTrip(principal.id(), tripPublicId);
    }

    @Test
    void requiresAuthenticationForAllTripEndpoints() throws Exception {

        UUID tripPublicId = UUID.randomUUID();

        String requestBody = objectMapper.writeValueAsString(validRequest());

        mockMvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/trips"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/trips")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/trips/{publicId}", tripPublicId)).andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/trips/{publicId}", tripPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/trips/{publicId}", tripPublicId)).andExpect(status().isUnauthorized());

        verifyNoInteractions(tripService);
    }

    @Test
    void rejectsMalformedEnumWithoutCallingService() throws Exception {

        UserPrincipal principal = userPrincipal();

        String invalidJson =
                """
                {
                  "tripDate": "2026-08-20",
                  "startTime": "08:00",
                  "endTime": "18:00",
                  "budget": 500000.00,
                  "startLocation": {
                    "label": "Chợ Bến Thành, Quận 1",
                    "latitude": 10.7726400,
                    "longitude": 106.6980500
                  },
                  "travelPace": "SLOW",
                  "environmentPreference": "MIXED"
                }
                """;

        mockMvc.perform(post("/api/v1/trips")
                        .with(authenticatedAs(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request body is malformed or contains an invalid value"));

        verifyNoInteractions(tripService);
    }

    @Test
    void rejectsMalformedPublicIdWithoutCallingService() throws Exception {

        UserPrincipal principal = userPrincipal();

        mockMvc.perform(get("/api/v1/trips/not-a-uuid").with(authenticatedAs(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("publicId"));

        verifyNoInteractions(tripService);
    }

    @Test
    void returnsTripNotFoundProblemDetail() throws Exception {

        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();

        when(tripService.getTrip(principal.id(), tripPublicId)).thenThrow(new TripNotFoundException());

        mockMvc.perform(get("/api/v1/trips/{publicId}", tripPublicId).with(authenticatedAs(principal)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Trip not found"))
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/v1/trips/" + tripPublicId));
    }

    @Test
    void returnsTripNotFoundProblemDetailWhenDeleting() throws Exception {
        UserPrincipal principal = userPrincipal();
        UUID tripPublicId = UUID.randomUUID();

        doThrow(new TripNotFoundException()).when(tripService).deleteTrip(principal.id(), tripPublicId);

        mockMvc.perform(delete("/api/v1/trips/{publicId}", tripPublicId).with(authenticatedAs(principal)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    private RequestPostProcessor authenticatedAs(UserPrincipal principal) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());

        return authentication(authenticationToken);
    }

    private UserPrincipal userPrincipal() {
        return new UserPrincipal(
                99L,
                UUID.fromString("8f047237-a9fb-4be0-becb-3cffaf445acd"),
                "tan@example.com",
                "Nguyễn Bá Tân",
                UserRole.USER);
    }

    private SaveTripRequest validRequest() {
        return new SaveTripRequest(
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                new StartLocationRequest(
                        "Chợ Bến Thành, Quận 1", new BigDecimal("10.7726400"), new BigDecimal("106.6980500")),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED);
    }

    private TripResponse tripResponse(UUID publicId) {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-03T10:00:00+07:00");

        return new TripResponse(
                publicId,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                new StartLocationResponse(
                        "Chợ Bến Thành, Quận 1", new BigDecimal("10.7726400"), new BigDecimal("106.6980500")),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                timestamp,
                timestamp);
    }

    private TripSummaryResponse tripSummaryResponse(UUID publicId) {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-03T10:00:00+07:00");

        return new TripSummaryResponse(
                publicId,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                "Chợ Bến Thành, Quận 1",
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                timestamp);
    }
}
