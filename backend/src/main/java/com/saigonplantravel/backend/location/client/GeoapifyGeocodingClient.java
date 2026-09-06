package com.saigonplantravel.backend.location.client;

import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import com.saigonplantravel.backend.location.exception.GeocodingUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GeoapifyGeocodingClient implements GeocodingClient {

    private static final int RESULT_LIMIT = 5;
    private static final String VIETNAM_FILTER = "countrycode:vn";
    private static final String HO_CHI_MINH_CITY_BIAS = "proximity:106.7009,10.7769";
    private static final String PROVIDER_UNAVAILABLE_MESSAGE = "Location search provider is unavailable";

    private final RestClient restClient;
    private final String apiKey;

    public GeoapifyGeocodingClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey.trim();
    }

    @Override
    public List<LocationSearchResponse> search(String query) {
        if (apiKey.isBlank()) {
            throw new GeocodingUnavailableException("Location search provider is not configured");
        }

        try {
            GeoapifyResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/geocode/search")
                            .queryParam("text", "{query}")
                            .queryParam("limit", RESULT_LIMIT)
                            .queryParam("filter", VIETNAM_FILTER)
                            .queryParam("bias", HO_CHI_MINH_CITY_BIAS)
                            .queryParam("lang", "vi")
                            .queryParam("format", "json")
                            .queryParam("apiKey", "{apiKey}")
                            .build(query, apiKey))
                    .retrieve()
                    .body(GeoapifyResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }

            return response.results().stream()
                    .filter(Objects::nonNull)
                    .filter(GeoapifySearchResult::hasUsableLocation)
                    .limit(RESULT_LIMIT)
                    .map(result -> new LocationSearchResponse(
                            limitLabel(result.formatted()),
                            result.latitude().setScale(7, RoundingMode.HALF_UP),
                            result.longitude().setScale(7, RoundingMode.HALF_UP)))
                    .toList();
        } catch (RestClientException ignored) {
            // Do not retain the provider exception because its message can contain the API-key query parameter.
            throw new GeocodingUnavailableException(PROVIDER_UNAVAILABLE_MESSAGE);
        }
    }

    private String limitLabel(String formattedAddress) {
        String label = formattedAddress.trim();
        return label.length() <= 255 ? label : label.substring(0, 255).trim();
    }

    private record GeoapifyResponse(List<GeoapifySearchResult> results) {}

    private record GeoapifySearchResult(String formatted, BigDecimal lat, BigDecimal lon) {

        private BigDecimal latitude() {
            return lat;
        }

        private BigDecimal longitude() {
            return lon;
        }

        private boolean hasUsableLocation() {
            return formatted != null
                    && !formatted.isBlank()
                    && latitude() != null
                    && longitude() != null
                    && latitude().compareTo(BigDecimal.valueOf(-90)) >= 0
                    && latitude().compareTo(BigDecimal.valueOf(90)) <= 0
                    && longitude().compareTo(BigDecimal.valueOf(-180)) >= 0
                    && longitude().compareTo(BigDecimal.valueOf(180)) <= 0;
        }
    }
}
