package com.saigonplantravel.backend.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saigonplantravel.backend.place.dto.PageResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.admin.PlaceCreateRequest;
import com.saigonplantravel.backend.place.dto.admin.PlaceUpdateRequest;
import com.saigonplantravel.backend.place.exception.PlaceSlugAlreadyExistsException;
import com.saigonplantravel.backend.place.service.PlaceImageService;
import com.saigonplantravel.backend.place.service.PlaceService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AdminPlaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPlaceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private PlaceImageService placeImageService;

    @Test
    void rendersPlaceListFromExistingPlaceService() throws Exception {
        PageResponse<AdminPlaceSummaryResponse> page = new PageResponse<>(
                List.of(new AdminPlaceSummaryResponse(
                        1L,
                        "Demo Place",
                        "demo-place",
                        "Demo data",
                        new BigDecimal("10.0000000"),
                        new BigDecimal("106.0000000"),
                        60,
                        BigDecimal.ZERO,
                        new BigDecimal("100000.00"),
                        true,
                        false)),
                2,
                10,
                21,
                3,
                false,
                true);
        when(placeService.getPlacesForAdministration(null, 2, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/places").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/list"))
                .andExpect(model().attribute("placesPage", page))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Demo Place")));

        verify(placeService).getPlacesForAdministration(null, 2, 10);
    }

    @Test
    void usesDefaultPaginationForPlaceList() throws Exception {
        PageResponse<AdminPlaceSummaryResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(placeService.getPlacesForAdministration(null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/admin/places"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("placesPage", page));

        verify(placeService).getPlacesForAdministration(null, 0, 20);
    }

    @Test
    void normalizesKeywordAndPreservesItInPaginationLinks() throws Exception {
        PageResponse<AdminPlaceSummaryResponse> page = new PageResponse<>(List.of(), 1, 10, 25, 3, false, false);
        when(placeService.getPlacesForAdministration("Bảo tàng", 1, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/places")
                        .param("keyword", "  Bảo   tàng  ")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", "Bảo tàng"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No places match your search.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("keyword=B%E1%BA%A3o%20t%C3%A0ng")));

        verify(placeService).getPlacesForAdministration("Bảo tàng", 1, 10);
    }

    @Test
    void treatsBlankKeywordAsNoSearch() throws Exception {
        PageResponse<AdminPlaceSummaryResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(placeService.getPlacesForAdministration(null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/admin/places").param("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", org.hamcrest.Matchers.nullValue()))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No places found.")));

        verify(placeService).getPlacesForAdministration(null, 0, 20);
    }

    @Test
    void rejectsInvalidAdminPagination() throws Exception {
        mockMvc.perform(get("/admin/places").param("page", "-1").param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(placeService, never())
                .getPlacesForAdministration(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rendersPlaceDetailAndCorrectStatusAction() throws Exception {
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(adminPlaceDetail(false));

        mockMvc.perform(get("/admin/places/demo-art-space"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/detail"))
                .andExpect(model().attributeExists("place"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Activate Place")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Deactivate Place"))));
    }

    @Test
    void uploadsPlaceCoverAndRedirectsToDetail() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "cover.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/admin/places/demo-art-space/cover-image").file(image))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeImageService).uploadCover("demo-art-space", image);
    }

    @Test
    void removesPlaceCoverAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/cover-image/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeImageService).removeCover("demo-art-space");
    }

    @Test
    void rendersDeactivateActionOnlyForActivePlace() throws Exception {
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(adminPlaceDetail(true));

        mockMvc.perform(get("/admin/places/demo-art-space"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Deactivate Place")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Activate Place"))));
    }

    @Test
    void activatesPlaceAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/activate"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeService).activatePlace("demo-art-space");
    }

    @Test
    void deactivatesPlaceAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/deactivate"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeService).deactivatePlace("demo-art-space");
    }

    @Test
    void rendersCreatePlaceForm() throws Exception {
        mockMvc.perform(get("/admin/places/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/create-form"))
                .andExpect(model().attributeExists("placeForm"));
    }

    @Test
    void createsPlaceAndRedirectsToDetail() throws Exception {
        PlaceCreateRequest request = validCreateRequest();
        when(placeService.createPlace(request)).thenReturn(createdPlaceDetail());

        mockMvc.perform(validCreatePost())
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places/new-place"));

        verify(placeService).createPlace(request);
    }

    @Test
    void preservesCreateFormForInvalidCostRange() throws Exception {
        mockMvc.perform(invalidCreatePost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/create-form"))
                .andExpect(model().attributeHasFieldErrors("placeForm", "slug", "maxCost"));
        verify(placeService, never()).createPlace(any());
    }

    @Test
    void preservesCreateFormForDuplicateSlug() throws Exception {
        PlaceCreateRequest request = validCreateRequest();
        when(placeService.createPlace(request)).thenThrow(new PlaceSlugAlreadyExistsException());
        mockMvc.perform(validCreatePost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/create-form"))
                .andExpect(model().attributeHasFieldErrors("placeForm", "slug"));
    }

    @Test
    void rendersEditFormWithoutEditableSlugOrStatus() throws Exception {
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(adminPlaceDetail(false));

        mockMvc.perform(get("/admin/places/demo-art-space/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/edit-form"))
                .andExpect(model().attribute("placeSlug", "demo-art-space"))
                .andExpect(model().attribute("placeActive", false))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"slug\""))))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"active\""))));
    }

    @Test
    void updatesPlaceAndRedirectsToImmutableSlug() throws Exception {
        PlaceUpdateRequest request = validUpdateRequest();
        when(placeService.updatePlace("demo-art-space", request)).thenReturn(adminPlaceDetail(false));

        mockMvc.perform(validUpdatePost())
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places/demo-art-space"));

        verify(placeService).updatePlace("demo-art-space", request);
    }

    @Test
    void preservesEditFormAndRejectsInvalidCostRange() throws Exception {
        when(placeService.getPlaceDetailForAdministrationBySlug("demo-art-space"))
                .thenReturn(adminPlaceDetail(false));

        mockMvc.perform(invalidUpdatePost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/edit-form"))
                .andExpect(model().attributeHasFieldErrors("placeForm", "maxCost"))
                .andExpect(model().attribute("placeSlug", "demo-art-space"));

        verify(placeService, never()).updatePlace(any(String.class), any(PlaceUpdateRequest.class));
    }

    private MockHttpServletRequestBuilder validCreatePost() {
        return post("/admin/places")
                .param("name", "  New Place  ")
                .param("slug", "  new-place  ")
                .param("shortDescription", "  Short description  ")
                .param("fullDescription", "   ")
                .param("address", "  New address  ")
                .param("latitude", "10.7750000")
                .param("longitude", "106.7000000")
                .param("estimatedVisitMinutes", "90")
                .param("minCost", "50000.00")
                .param("maxCost", "150000.00")
                .param("indoor", "true");
    }

    private MockHttpServletRequestBuilder invalidCreatePost() {
        return post("/admin/places")
                .param("name", "New Place")
                .param("slug", "INVALID SLUG")
                .param("address", "New address")
                .param("latitude", "10.7750000")
                .param("longitude", "106.7000000")
                .param("estimatedVisitMinutes", "90")
                .param("minCost", "200000.00")
                .param("maxCost", "100000.00")
                .param("indoor", "true");
    }

    private MockHttpServletRequestBuilder validUpdatePost() {
        return post("/admin/places/demo-art-space")
                .param("name", "  Updated Place  ")
                .param("shortDescription", "  Updated short description  ")
                .param("fullDescription", "   ")
                .param("address", "  Updated address  ")
                .param("latitude", "10.7760000")
                .param("longitude", "106.7010000")
                .param("estimatedVisitMinutes", "120")
                .param("minCost", "60000.00")
                .param("maxCost", "160000.00")
                .param("indoor", "false");
    }

    private MockHttpServletRequestBuilder invalidUpdatePost() {
        return post("/admin/places/demo-art-space")
                .param("name", "Updated Place")
                .param("address", "Updated address")
                .param("latitude", "10.7760000")
                .param("longitude", "106.7010000")
                .param("estimatedVisitMinutes", "120")
                .param("minCost", "200000.00")
                .param("maxCost", "100000.00")
                .param("indoor", "false");
    }

    private PlaceCreateRequest validCreateRequest() {
        return new PlaceCreateRequest(
                "New Place",
                "new-place",
                "Short description",
                null,
                "New address",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true);
    }

    private PlaceUpdateRequest validUpdateRequest() {
        return new PlaceUpdateRequest(
                "Updated Place",
                "Updated short description",
                null,
                "Updated address",
                new BigDecimal("10.7760000"),
                new BigDecimal("106.7010000"),
                120,
                new BigDecimal("60000.00"),
                new BigDecimal("160000.00"),
                false);
    }

    private AdminPlaceDetailResponse adminPlaceDetail(boolean active) {
        return new AdminPlaceDetailResponse(
                1L,
                "Demo Art Space",
                "demo-art-space",
                "Short demo description",
                "Full demo description",
                "Demo address",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true,
                List.of(),
                List.of(),
                active);
    }

    private PlaceDetailResponse createdPlaceDetail() {
        PlaceCreateRequest request = validCreateRequest();
        return new PlaceDetailResponse(
                1L,
                request.name(),
                request.slug(),
                request.shortDescription(),
                request.fullDescription(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.estimatedVisitMinutes(),
                request.minCost(),
                request.maxCost(),
                request.indoor(),
                List.of(),
                List.of());
    }
}
