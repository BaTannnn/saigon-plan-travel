package com.saigonplantravel.backend.location.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import com.saigonplantravel.backend.location.exception.GeocodingUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NominatimGeocodingClient implements GeocodingClient {

    private static final int RESULT_LIMIT = 5;
    private static final String HO_CHI_MINH_CITY_VIEWBOX = "106.35,11.20,107.10,10.35";

    private final RestClient restClient;
    private final long minimumIntervalNanos;
    private final Object rateLimitMonitor = new Object();
    private long nextAllowedRequestNanos;

    public NominatimGeocodingClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.user-agent}") String userAgent,
            @Value("${app.geocoding.minimum-interval:1s}") java.time.Duration minimumInterval) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
        this.minimumIntervalNanos = minimumInterval.toNanos();
    }

    @Override
    public List<LocationSearchResponse> search(String query) {
        synchronized (rateLimitMonitor) {
            waitForRateLimit();
            nextAllowedRequestNanos = System.nanoTime() + minimumIntervalNanos;

            try {
                NominatimSearchResult[] response = restClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", "{query}")
                                .queryParam("format", "jsonv2")
                                .queryParam("limit", RESULT_LIMIT)
                                .queryParam("countrycodes", "vn")
                                .queryParam("viewbox", HO_CHI_MINH_CITY_VIEWBOX)
                                .queryParam("bounded", 0)
                                .queryParam("accept-language", "vi")
                                .build(query))
                        .retrieve()
                        .body(NominatimSearchResult[].class);

                if (response == null) {
                    return List.of();
                }

                return Arrays.stream(response)
                        .filter(NominatimSearchResult::hasUsableLocation)
                        .limit(RESULT_LIMIT)
                        .map(result -> new LocationSearchResponse(
                                limitLabel(result.displayName()),
                                result.latitude().setScale(7, RoundingMode.HALF_UP),
                                result.longitude().setScale(7, RoundingMode.HALF_UP)))
                        .toList();
            } catch (RestClientException exception) {
                throw new GeocodingUnavailableException("Location search provider is unavailable", exception);
            }
        }
    }

    private String limitLabel(String displayName) {
        String label = displayName.trim();
        return label.length() <= 255 ? label : label.substring(0, 255).trim();
    }

    private void waitForRateLimit() {
        long remainingNanos = nextAllowedRequestNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return;
        }

        try {
            TimeUnit.NANOSECONDS.sleep(remainingNanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GeocodingUnavailableException("Location search was interrupted", exception);
        }
    }

    private record NominatimSearchResult(
            @JsonProperty("display_name") String displayName,
            @JsonProperty("lat") BigDecimal latitude,
            @JsonProperty("lon") BigDecimal longitude) {

        private boolean hasUsableLocation() {
            return displayName != null
                    && !displayName.isBlank()
                    && latitude != null
                    && longitude != null
                    && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                    && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
                    && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                    && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
        }
    }
}
