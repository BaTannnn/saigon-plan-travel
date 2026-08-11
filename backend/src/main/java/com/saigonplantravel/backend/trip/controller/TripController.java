package com.saigonplantravel.backend.trip.controller;

import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.service.TripService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody SaveTripRequest request) {
        TripResponse response = tripService.createTrip(principal.id(), request);

        URI location = URI.create("/api/v1/trips/" + response.publicId());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<TripSummaryResponse> listTrips(@AuthenticationPrincipal UserPrincipal principal) {
        return tripService.listTrips(principal.id());
    }

    @GetMapping("/{publicId}")
    public TripResponse getTrip(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID publicId) {
        return tripService.getTrip(principal.id(), publicId);
    }

    @PutMapping("/{publicId}")
    public TripResponse replaceTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID publicId,
            @Valid @RequestBody SaveTripRequest request) {
        return tripService.replaceTrip(principal.id(), publicId, request);
    }
}
