package com.saigonplantravel.backend.location.service;

import com.saigonplantravel.backend.location.client.GeocodingClient;
import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocationSearchService {

    private final GeocodingClient geocodingClient;

    public LocationSearchService(GeocodingClient geocodingClient) {
        this.geocodingClient = geocodingClient;
    }

    public List<LocationSearchResponse> search(String query) {
        return geocodingClient.search(query.trim());
    }
}
