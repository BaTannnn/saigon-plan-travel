package com.saigonplantravel.backend.location.client;

import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import java.util.List;

public interface GeocodingClient {

    List<LocationSearchResponse> search(String query);
}
