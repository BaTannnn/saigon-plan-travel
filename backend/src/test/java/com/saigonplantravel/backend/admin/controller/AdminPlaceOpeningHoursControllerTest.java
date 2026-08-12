package com.saigonplantravel.backend.admin.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourState;
import com.saigonplantravel.backend.place.service.PlaceService;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminPlaceOpeningHoursController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPlaceOpeningHoursControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @Test
    void rendersCompleteOpeningHoursWeekAndCurrentStates() throws Exception {
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(placeWithOpeningHours());

        mockMvc.perform(get("/admin/places/demo-art-space/opening-hours/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/opening-hours-form"))
                .andExpect(model().attributeExists("openingHoursForm", "openingHourStates"));
    }

    @Test
    void replacesOpeningHoursAndRedirectsToDetail() throws Exception {
        mockMvc.perform(openingHoursPost("09:00", "17:00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeService)
                .replacePlaceOpeningHours(
                        eq("demo-art-space"),
                        argThat(days -> days.size() == 7
                                && days.get(0).getState() == OpeningHourState.OPEN
                                && days.get(0).getOpenTime().equals(LocalTime.of(9, 0))
                                && days.get(1).getState() == OpeningHourState.CLOSED
                                && days.get(2).getState() == OpeningHourState.UNKNOWN));
    }

    @Test
    void rejectsInvalidOpenTimeRangeOnOpeningHoursForm() throws Exception {
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(placeWithOpeningHours());

        mockMvc.perform(openingHoursPost("18:00", "09:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/opening-hours-form"))
                .andExpect(model().attributeHasErrors("openingHoursForm"));

        verify(placeService, never())
                .replacePlaceOpeningHours(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequestBuilder openingHoursPost(String openTime, String closeTime) {
        MockHttpServletRequestBuilder request = post("/admin/places/demo-art-space/opening-hours/edit");
        for (int index = 0; index < 7; index++) {
            request.param("days[" + index + "].dayOfWeek", Integer.toString(index + 1));
            request.param("days[" + index + "].state", index == 0 ? "OPEN" : index == 1 ? "CLOSED" : "UNKNOWN");
        }
        return request.param("days[0].openTime", openTime).param("days[0].closeTime", closeTime);
    }

    private AdminPlaceDetailResponse placeWithOpeningHours() {
        return new AdminPlaceDetailResponse(
                1L,
                "Demo Art Space",
                "demo-art-space",
                "Short",
                "Full",
                "Address",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true,
                List.of(),
                List.of(
                        new OpeningHourResponse((short) 1, false, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                        new OpeningHourResponse((short) 2, true, null, null)),
                false);
    }
}
