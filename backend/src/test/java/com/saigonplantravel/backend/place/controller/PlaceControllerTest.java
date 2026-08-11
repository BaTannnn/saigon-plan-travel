package com.saigonplantravel.backend.place.controller;

import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.common.exception.GlobalExceptionHandler;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import com.saigonplantravel.backend.common.security.SecurityConfig;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlacePageResponse;
import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(PlaceController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class
})
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @MockitoBean
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void usesDefaultPaginationAndReturnsLockedResponseContract() throws Exception {
        PlaceSummaryResponse place = demoPlace();
        when(placeService.searchPlaces(any(PlaceSearchRequest.class)))
                .thenReturn(new PlacePageResponse(List.of(place), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(7)))
                .andExpect(jsonPath("$.content[0]", aMapWithSize(10)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Demo Place"))
                .andExpect(jsonPath("$.content[0].slug").value("demo-place"))
                .andExpect(jsonPath("$.content[0].shortDescription").value(nullValue()))
                .andExpect(jsonPath("$.content[0].latitude").value(10.0000000))
                .andExpect(jsonPath("$.content[0].longitude").value(106.0000000))
                .andExpect(jsonPath("$.content[0].estimatedVisitMinutes").value(60))
                .andExpect(jsonPath("$.content[0].minCost").value(0))
                .andExpect(jsonPath("$.content[0].maxCost").value(100000))
                .andExpect(jsonPath("$.content[0].indoor").value(true))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].address").doesNotExist())
                .andExpect(jsonPath("$.content[0].fullDescription").doesNotExist())
                .andExpect(jsonPath("$.content[0].active").doesNotExist())
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist());
    }

    @Test
    void acceptsCustomPaginationAndAllowsEmptyPage() throws Exception {
        when(placeService.searchPlaces(any(PlaceSearchRequest.class)))
                .thenReturn(new PlacePageResponse(List.of(), 3, 10, 5, 1, false, true));

        mockMvc.perform(get("/api/v1/places").param("page", "3").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(3));
    }

    @Test
    void rejectsNegativePageWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Invalid pagination parameters"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("page must be greater than or equal to 0"))
                .andExpect(jsonPath("$.instance").value("/api/v1/places"));
    }

    @Test
    void rejectsZeroPageSizeWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid pagination parameters"))
                .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }

    @Test
    void rejectsPageSizeAboveOneHundredWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid pagination parameters"))
                .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }

    @Test
    void acceptsBoundaryPageSizes() throws Exception {
        when(placeService.searchPlaces(any(PlaceSearchRequest.class)))
                .thenReturn(
                        new PlacePageResponse(List.of(), 0, 1, 0, 0, true, true),
                        new PlacePageResponse(List.of(), 0, 100, 0, 0, true, true)
                );

        mockMvc.perform(get("/api/v1/places").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));
        mockMvc.perform(get("/api/v1/places").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void rejectsNonNumericPaginationWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("page", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("page and size must be valid integers"));
    }

    @Test
    void returnsSafeProblemDetailForUnexpectedErrors() throws Exception {
        when(placeService.searchPlaces(any(PlaceSearchRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("jdbc:postgresql://secret"));

        mockMvc.perform(get("/api/v1/places"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("jdbc:postgresql")
                )));
    }

    @Test
    void bindsAndNormalizesAllSearchFilters() throws Exception {
        when(placeService.searchPlaces(any(PlaceSearchRequest.class)))
                .thenReturn(new PlacePageResponse(List.of(), 2, 10, 0, 0, false, true));

        mockMvc.perform(get("/api/v1/places")
                        .param("keyword", "  Bảo   tàng  ")
                        .param("category", "van-hoa")
                        .param("indoor", "false")
                        .param("maxCost", "100000")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(7)));

        ArgumentCaptor<PlaceSearchRequest> requestCaptor =
                ArgumentCaptor.forClass(PlaceSearchRequest.class);
        verify(placeService).searchPlaces(requestCaptor.capture());
        PlaceSearchRequest request = requestCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.keyword()).isEqualTo("Bảo tàng");
        org.assertj.core.api.Assertions.assertThat(request.category()).isEqualTo("van-hoa");
        org.assertj.core.api.Assertions.assertThat(request.indoor()).isFalse();
        org.assertj.core.api.Assertions.assertThat(request.maxCost())
                .isEqualByComparingTo("100000");
        org.assertj.core.api.Assertions.assertThat(request.resolvedPage()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(request.resolvedSize()).isEqualTo(10);
    }

    @Test
    void returnsInvalidRequestWithFieldErrorsForSearchFilters() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("category", "Invalid-Slug"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));

        mockMvc.perform(get("/api/v1/places").param("indoor", "yes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("indoor"));

        mockMvc.perform(get("/api/v1/places").param("indoor", "TRUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("indoor"));

        mockMvc.perform(get("/api/v1/places").param("maxCost", "100000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("maxCost"));

        verifyNoInteractions(placeService);
    }

    @Test
    void returnsLockedPlaceDetailContractAndTimeFormat() throws Exception {
        PlaceDetailResponse detail = new PlaceDetailResponse(
                1L,
                "Demo Art Space",
                "demo-art-space",
                null,
                null,
                "Địa chỉ demo 1",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true,
                List.of(new CategoryResponse(1L, "Nghệ thuật", "nghe-thuat")),
                List.of(
                        new OpeningHourResponse(
                                (short) 1,
                                false,
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0)
                        ),
                        new OpeningHourResponse((short) 2, true, null, null)
                )
        );
        when(placeService.getPlaceDetailBySlug("demo-art-space")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/places/demo-art-space"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(14)))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Demo Art Space"))
                .andExpect(jsonPath("$.slug").value("demo-art-space"))
                .andExpect(jsonPath("$.shortDescription").value(nullValue()))
                .andExpect(jsonPath("$.fullDescription").value(nullValue()))
                .andExpect(jsonPath("$.address").value("Địa chỉ demo 1"))
                .andExpect(jsonPath("$.estimatedVisitMinutes").value(90))
                .andExpect(jsonPath("$.indoor").value(true))
                .andExpect(jsonPath("$.categories[0]", aMapWithSize(3)))
                .andExpect(jsonPath("$.categories[0].slug").value("nghe-thuat"))
                .andExpect(jsonPath("$.openingHours[0]", aMapWithSize(4)))
                .andExpect(jsonPath("$.openingHours[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.openingHours[0].openTime").value("09:00"))
                .andExpect(jsonPath("$.openingHours[0].closeTime").value("17:00"))
                .andExpect(jsonPath("$.openingHours[1].closed").value(true))
                .andExpect(jsonPath("$.openingHours[1].openTime").value(nullValue()))
                .andExpect(jsonPath("$.openingHours[1].closeTime").value(nullValue()))
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void returnsSameProblemDetailForAnySlugWithoutActivePlace() throws Exception {
        when(placeService.getPlaceDetailBySlug(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new PlaceNotFoundException());

        for (String slug : List.of(
                "slug-khong-ton-tai",
                "demo-temporarily-hidden-place",
                "INVALID_SLUG"
        )) {
            mockMvc.perform(get("/api/v1/places/{slug}", slug))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/problem+json"))
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Place not found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Place not found"))
                    .andExpect(jsonPath("$.instance").value("/api/v1/places/" + slug))
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }
    }

    private PlaceSummaryResponse demoPlace() {
        return new PlaceSummaryResponse(
                1L,
                "Demo Place",
                "demo-place",
                null,
                new BigDecimal("10.0000000"),
                new BigDecimal("106.0000000"),
                60,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                true
        );
    }
}
