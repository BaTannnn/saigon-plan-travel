package com.saigonplantravel.backend.admin.controller;

import com.saigonplantravel.backend.place.dto.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourState;
import com.saigonplantravel.backend.place.dto.PlaceOpeningHoursRequest;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/places/{slug}/opening-hours/edit")
public class AdminPlaceOpeningHoursController {
    private final PlaceService placeService;

    public AdminPlaceOpeningHoursController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    String editPlaceOpeningHoursPage(@PathVariable String slug, Model model) {
        AdminPlaceDetailResponse place = placeService.getPlaceDetailForAdministrationBySlug(slug);
        model.addAttribute("openingHoursForm", toOpeningHoursRequest(place));
        addPageAttributes(model, place);
        return "admin/places/opening-hours-form";
    }

    @PostMapping
    String replacePlaceOpeningHours(
            @PathVariable String slug,
            @Valid @ModelAttribute("openingHoursForm") PlaceOpeningHoursRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addPageAttributes(model, placeService.getPlaceDetailForAdministrationBySlug(slug));
            return "admin/places/opening-hours-form";
        }

        placeService.replacePlaceOpeningHours(slug, request.getDays());
        return "redirect:/admin/places/" + slug;
    }

    private PlaceOpeningHoursRequest toOpeningHoursRequest(AdminPlaceDetailResponse place) {
        Map<Short, OpeningHourResponse> existingHours = place.openingHours().stream()
                .collect(Collectors.toMap(OpeningHourResponse::dayOfWeek, Function.identity()));
        ArrayList<PlaceOpeningHoursRequest.Day> days = new ArrayList<>();
        for (short dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            OpeningHourResponse existingHour = existingHours.get(dayOfWeek);
            OpeningHourState state = existingHour == null
                    ? OpeningHourState.UNKNOWN
                    : existingHour.closed() ? OpeningHourState.CLOSED : OpeningHourState.OPEN;
            days.add(new PlaceOpeningHoursRequest.Day(
                    dayOfWeek,
                    state,
                    existingHour == null ? null : existingHour.openTime(),
                    existingHour == null ? null : existingHour.closeTime()));
        }
        return new PlaceOpeningHoursRequest(days);
    }

    private void addPageAttributes(Model model, AdminPlaceDetailResponse place) {
        model.addAttribute("place", place);
        model.addAttribute("openingHourStates", OpeningHourState.values());
    }
}
