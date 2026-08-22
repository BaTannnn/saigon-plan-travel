package com.saigonplantravel.backend.admin.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saigonplantravel.backend.place.dto.admin.AdminCategoryResponse;
import com.saigonplantravel.backend.place.dto.admin.CategoryCreateRequest;
import com.saigonplantravel.backend.place.dto.admin.CategoryUpdateRequest;
import com.saigonplantravel.backend.place.exception.CategoryAlreadyExistsException;
import com.saigonplantravel.backend.place.exception.CategoryNotFoundException;
import com.saigonplantravel.backend.place.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void rendersAdministrativeCategoryCatalog() throws Exception {
        List<AdminCategoryResponse> categories = List.of(
                new AdminCategoryResponse(1L, "Art", "art", "Art places"),
                new AdminCategoryResponse(2L, "Culture", "culture", null));
        when(categoryService.getCategoriesForAdministration()).thenReturn(categories);

        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/list"))
                .andExpect(model().attribute("categories", categories))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Art places")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No description")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/categories/art/edit")));
    }

    @Test
    void rendersCreateCategoryForm() throws Exception {
        mockMvc.perform(get("/admin/categories/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create-form"))
                .andExpect(model().attributeExists("categoryForm"));
    }

    @Test
    void createsCategoryAndRedirectsToCatalogAnchor() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest("Science", "science", null);
        when(categoryService.createCategory(request))
                .thenReturn(new AdminCategoryResponse(6L, "Science", "science", null));

        mockMvc.perform(validCategoryPost())
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/categories#category-science"));

        verify(categoryService).createCategory(request);
    }

    @Test
    void preservesCreateCategoryFormWhenInputIsInvalid() throws Exception {
        mockMvc.perform(post("/admin/categories").param("name", " ").param("slug", "INVALID SLUG"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create-form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name", "slug"));

        verify(categoryService, never()).createCategory(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsDuplicateCategoryNameOnTheForm() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest("Science", "science", null);
        doThrow(new CategoryAlreadyExistsException("name"))
                .when(categoryService)
                .createCategory(request);

        mockMvc.perform(validCategoryPost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create-form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Category name already exists")));
    }

    @Test
    void rendersEditCategoryFormWithImmutableSlug() throws Exception {
        when(categoryService.getCategoryForAdministrationBySlug("science"))
                .thenReturn(new AdminCategoryResponse(6L, "Science", "science", "Science places"));

        mockMvc.perform(get("/admin/categories/science/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/edit-form"))
                .andExpect(model().attribute("categoryForm", new CategoryUpdateRequest("Science", "Science places")))
                .andExpect(model().attribute("categorySlug", "science"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Science places")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"slug\""))));
    }

    @Test
    void updatesCategoryAndRedirectsToImmutableSlug() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest("Natural Science", null);
        when(categoryService.updateCategory("science", request))
                .thenReturn(new AdminCategoryResponse(6L, "Natural Science", "science", null));

        mockMvc.perform(validCategoryUpdatePost())
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/categories#category-science"));

        verify(categoryService).updateCategory("science", request);
    }

    @Test
    void preservesEditCategoryFormWhenInputIsInvalid() throws Exception {
        mockMvc.perform(post("/admin/categories/science").param("name", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/edit-form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"))
                .andExpect(model().attribute("categorySlug", "science"));

        verify(categoryService, never())
                .updateCategory(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsDuplicateCategoryNameOnEditForm() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest("Natural Science", null);
        doThrow(new CategoryAlreadyExistsException("name"))
                .when(categoryService)
                .updateCategory("science", request);

        mockMvc.perform(validCategoryUpdatePost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/edit-form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"))
                .andExpect(model().attribute("categorySlug", "science"));
    }

    @Test
    void returnsNotFoundForMissingCategory() throws Exception {
        when(categoryService.getCategoryForAdministrationBySlug("missing")).thenThrow(new CategoryNotFoundException());

        mockMvc.perform(get("/admin/categories/missing/edit")).andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validCategoryPost() {
        return post("/admin/categories")
                .param("name", "  Science  ")
                .param("slug", "  science  ")
                .param("description", "   ");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validCategoryUpdatePost() {
        return post("/admin/categories/science")
                .param("name", "  Natural Science  ")
                .param("description", "   ");
    }
}
