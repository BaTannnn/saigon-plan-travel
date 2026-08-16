package com.saigonplantravel.backend.admin.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.PlaceCategoryAssignmentRequest;
import com.saigonplantravel.backend.place.exception.InvalidPlaceCategoryAssignmentException;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.place.service.PlaceService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPlaceCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPlaceCategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void rendersAvailableCategoriesAndCurrentAssignments() throws Exception {
        AdminPlaceDetailResponse place = place(List.of(new CategoryResponse(1L, "Art", "art")));
        List<CategoryResponse> available =
                List.of(new CategoryResponse(1L, "Art", "art"), new CategoryResponse(2L, "Culture", "culture"));
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(place);
        when(categoryService.getCategories()).thenReturn(available);

        mockMvc.perform(get("/admin/places/demo-art-space/categories/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/categories-form"))
                .andExpect(model().attribute("categoryForm", new PlaceCategoryAssignmentRequest(List.of("art"))))
                .andExpect(model().attribute("availableCategories", available));
    }

    @Test
    void replacesPlaceCategoriesAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/categories/edit")
                        .param("categorySlugs", " art ", "culture", "art"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeService).replacePlaceCategories("demo-art-space", List.of("art", "culture"));
    }

    @Test
    void allowsRemovingEveryPlaceCategory() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/categories/edit")).andExpect(status().isFound());
        verify(placeService).replacePlaceCategories("demo-art-space", List.of());
    }

    @Test
    void reportsUnknownCategoryOnTheForm() throws Exception {
        doThrow(new InvalidPlaceCategoryAssignmentException(List.of("missing")))
                .when(placeService)
                .replacePlaceCategories("demo-art-space", List.of("missing"));
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(place(List.of()));
        when(categoryService.getCategories()).thenReturn(List.of());

        mockMvc.perform(post("/admin/places/demo-art-space/categories/edit").param("categorySlugs", "missing"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/categories-form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "categorySlugs"));
    }

    private AdminPlaceDetailResponse place(List<CategoryResponse> categories) {
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
                categories,
                List.of(),
                false);
    }
}
