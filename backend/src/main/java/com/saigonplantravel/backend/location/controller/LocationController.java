package com.saigonplantravel.backend.location.controller;

import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import com.saigonplantravel.backend.location.service.LocationSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationSearchService locationSearchService;

    public LocationController(LocationSearchService locationSearchService) {
        this.locationSearchService = locationSearchService;
    }

    @GetMapping("/search")
    public List<LocationSearchResponse> search(
            @RequestParam("q")
                    @NotBlank(message = "q must not be blank")
                    @Size(max = 120, message = "q must contain at most 120 characters")
                    String query) {
        return locationSearchService.search(query);
    }
}
