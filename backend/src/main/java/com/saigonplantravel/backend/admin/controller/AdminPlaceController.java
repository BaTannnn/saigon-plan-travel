package com.saigonplantravel.backend.admin.controller;

import com.saigonplantravel.backend.media.MediaStorageException;
import com.saigonplantravel.backend.place.dto.PageResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.admin.PlaceCreateRequest;
import com.saigonplantravel.backend.place.dto.admin.PlaceUpdateRequest;
import com.saigonplantravel.backend.place.exception.PlaceImageException;
import com.saigonplantravel.backend.place.exception.PlaceSlugAlreadyExistsException;
import com.saigonplantravel.backend.place.service.PlaceImageService;
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
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin")
public class AdminPlaceController {
    private final PlaceService placeService;
    private final PlaceImageService placeImageService;

    public AdminPlaceController(PlaceService placeService, PlaceImageService placeImageService) {
        this.placeService = placeService;
        this.placeImageService = placeImageService;
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
        addDetailPageAttributes(model, slug);
        return "admin/places/detail";
    }

    @PostMapping("/places/{slug}/cover-image")
    String uploadCoverImage(@PathVariable String slug, @RequestParam("image") MultipartFile image, Model model) {
        try {
            placeImageService.uploadCover(slug, image);
            return "redirect:/admin/places/" + slug;
        } catch (PlaceImageException | MediaStorageException exception) {
            addDetailPageAttributes(model, slug);
            model.addAttribute("imageError", exception.getMessage());
            return "admin/places/detail";
        }
    }

    @PostMapping("/places/{slug}/cover-image/remove")
    String removeCoverImage(@PathVariable String slug, Model model) {
        try {
            placeImageService.removeCover(slug);
            return "redirect:/admin/places/" + slug;
        } catch (PlaceImageException | MediaStorageException exception) {
            addDetailPageAttributes(model, slug);
            model.addAttribute("imageError", exception.getMessage());
            return "admin/places/detail";
        }
    }

    private void addDetailPageAttributes(Model model, String slug) {
        model.addAttribute("place", placeService.getPlaceDetailForAdministrationBySlug(slug));
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
