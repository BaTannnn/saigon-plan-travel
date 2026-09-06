package com.saigonplantravel.backend.admin.controller;

import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.PlaceCategoryAssignmentRequest;
import com.saigonplantravel.backend.place.exception.InvalidPlaceCategoryAssignmentException;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/places/{slug}/categories/edit")
public class AdminPlaceCategoryController {
    private final PlaceService placeService;
    private final CategoryService categoryService;

    public AdminPlaceCategoryController(PlaceService placeService, CategoryService categoryService) {
        this.placeService = placeService;
        this.categoryService = categoryService;
    }

    @GetMapping
    String editPlaceCategoriesPage(@PathVariable String slug, Model model) {
        AdminPlaceDetailResponse place = placeService.getPlaceDetailForAdministrationBySlug(slug);
        PlaceCategoryAssignmentRequest request = new PlaceCategoryAssignmentRequest(
                place.categories().stream().map(category -> category.slug()).toList());
        model.addAttribute("categoryForm", request);
        addPageAttributes(model, place);
        return "admin/places/categories-form";
    }

    @PostMapping
    String replacePlaceCategories(
            @PathVariable String slug,
            @Valid @ModelAttribute("categoryForm") PlaceCategoryAssignmentRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addPageAttributes(model, placeService.getPlaceDetailForAdministrationBySlug(slug));
            return "admin/places/categories-form";
        }

        try {
            placeService.replacePlaceCategories(slug, request.categorySlugs());
            return "redirect:/admin/places/" + slug;
        } catch (InvalidPlaceCategoryAssignmentException exception) {
            bindingResult.rejectValue("categorySlugs", "place.categories.unknown", exception.getMessage());
            addPageAttributes(model, placeService.getPlaceDetailForAdministrationBySlug(slug));
            return "admin/places/categories-form";
        }
    }

    private void addPageAttributes(Model model, AdminPlaceDetailResponse place) {
        model.addAttribute("place", place);
        model.addAttribute("availableCategories", categoryService.getCategories());
    }
}
