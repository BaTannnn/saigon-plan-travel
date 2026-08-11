package com.saigonplantravel.backend.place.controller;

import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlacePageResponse;
import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public PlacePageResponse getPlaces(@Valid @ModelAttribute PlaceSearchRequest request) {
        return placeService.searchPlaces(request);
    }

    @GetMapping("/{slug}")
    public PlaceDetailResponse getPlaceDetail(@PathVariable String slug) {
        return placeService.getPlaceDetailBySlug(slug);
    }

    @InitBinder
    void configureStrictBooleanBinding(WebDataBinder binder) {
        binder.registerCustomEditor(Boolean.class, "indoor", new StrictBooleanEditor());
    }

    private static final class StrictBooleanEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            if (text == null || text.isEmpty()) {
                setValue(null);
            } else if ("true".equals(text)) {
                setValue(Boolean.TRUE);
            } else if ("false".equals(text)) {
                setValue(Boolean.FALSE);
            } else {
                throw new IllegalArgumentException("indoor must be true or false");
            }
        }
    }
}
