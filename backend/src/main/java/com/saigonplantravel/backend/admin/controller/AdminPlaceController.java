package com.saigonplantravel.backend.admin.controller;

import com.saigonplantravel.backend.place.dto.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.AdminPlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.PageResponse;
import com.saigonplantravel.backend.place.dto.PlaceCreateRequest;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlaceUpdateRequest;
import com.saigonplantravel.backend.place.exception.PlaceSlugAlreadyExistsException;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminPlaceController {
    private final PlaceService placeService;

    public AdminPlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/places")
    String placesPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Model model) {
        PageResponse<AdminPlaceSummaryResponse> placesPage = placeService.getPlacesForAdministration(page, size);
        model.addAttribute("placesPage", placesPage);
        return "admin/places/list";
    }

    @GetMapping("/places/new")
    String newPlacePage(Model model) {
        model.addAttribute(
                "placeForm", new PlaceCreateRequest(null, null, null, null, null, null, null, null, null, null, null));
        return "admin/places/create-form";
    }

    @PostMapping("/places")
    String createPlace(@Valid @ModelAttribute("placeForm") PlaceCreateRequest request, BindingResult bindingResult) {
        if (request.hasInvalidCostRange()) {
            bindingResult.rejectValue(
                    "maxCost", "place.maxCost.range", "must be greater than or equal to minimum cost");
        }
        if (bindingResult.hasErrors()) {
            return "admin/places/create-form";
        }

        try {
            PlaceDetailResponse createdPlace = placeService.createPlace(request);
            return "redirect:/admin/places/" + createdPlace.slug();
        } catch (PlaceSlugAlreadyExistsException exception) {
            bindingResult.rejectValue("slug", "place.slug.duplicate", exception.getMessage());
            return "admin/places/create-form";
        }
    }

    @GetMapping("/places/{slug}/edit")
    String editPlacePage(@PathVariable String slug, Model model) {
        AdminPlaceDetailResponse place = placeService.getPlaceDetailForAdministrationBySlug(slug);
        model.addAttribute("placeForm", toUpdateRequest(place));
        addEditPageAttributes(model, place.slug(), place.active());
        return "admin/places/edit-form";
    }

    @PostMapping("/places/{slug}")
    String updatePlace(
            @PathVariable String slug,
            @Valid @ModelAttribute("placeForm") PlaceUpdateRequest request,
            BindingResult bindingResult,
            Model model) {
        if (request.hasInvalidCostRange()) {
            bindingResult.rejectValue(
                    "maxCost", "place.maxCost.range", "must be greater than or equal to minimum cost");
        }
        if (bindingResult.hasErrors()) {
            AdminPlaceDetailResponse place = placeService.getPlaceDetailForAdministrationBySlug(slug);
            addEditPageAttributes(model, place.slug(), place.active());
            return "admin/places/edit-form";
        }

        AdminPlaceDetailResponse updatedPlace = placeService.updatePlace(slug, request);
        return "redirect:/admin/places/" + updatedPlace.slug();
    }

    @PostMapping("/places/{slug}/activate")
    String activatePlace(@PathVariable String slug) {
        placeService.activatePlace(slug);
        return "redirect:/admin/places/" + slug;
    }

    @PostMapping("/places/{slug}/deactivate")
    String deactivatePlace(@PathVariable String slug) {
        placeService.deactivatePlace(slug);
        return "redirect:/admin/places/" + slug;
    }

    @GetMapping("/places/{slug}")
    String placeDetailPage(@PathVariable String slug, Model model) {
        AdminPlaceDetailResponse place = placeService.getPlaceDetailForAdministrationBySlug(slug);
        model.addAttribute("place", place);
        return "admin/places/detail";
    }

    private PlaceUpdateRequest toUpdateRequest(AdminPlaceDetailResponse place) {
        return new PlaceUpdateRequest(
                place.name(),
                place.shortDescription(),
                place.fullDescription(),
                place.address(),
                place.latitude(),
                place.longitude(),
                place.estimatedVisitMinutes(),
                place.minCost(),
                place.maxCost(),
                place.indoor());
    }

    private void addEditPageAttributes(Model model, String slug, Boolean active) {
        model.addAttribute("placeSlug", slug);
        model.addAttribute("placeActive", active);
    }
}
