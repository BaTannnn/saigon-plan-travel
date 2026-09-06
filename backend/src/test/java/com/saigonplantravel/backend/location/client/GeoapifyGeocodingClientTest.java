package com.saigonplantravel.backend.location.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import com.saigonplantravel.backend.location.exception.GeocodingUnavailableException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeoapifyGeocodingClientTest {

    private static final String API_KEY = "test-api-key";

    @Test
    void sendsScopedSearchAndMapsOnlyOwnedContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeoapifyGeocodingClient client = new GeoapifyGeocodingClient(builder, "https://geoapify.test", API_KEY);

        server.expect(once(), requestTo(containsString("/v1/geocode/search?")))
                .andExpect(requestTo(not(containsString("%2520"))))
                .andExpect(queryParam("text", "Dinh%20%C4%90%E1%BB%99c%20L%E1%BA%ADp"))
                .andExpect(queryParam("limit", "5"))
                .andExpect(queryParam("filter", "countrycode:vn"))
                .andExpect(queryParam("bias", "proximity:106.7009,10.7769"))
                .andExpect(queryParam("lang", "vi"))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("apiKey", API_KEY))
                .andRespond(withSuccess(
                        """
                        {
                          "results": [
                            {
                              "formatted": "Dinh Độc Lập, Quận 1, Thành phố Hồ Chí Minh, Việt Nam",
                              "lat": 10.7770754,
                              "lon": 106.6952941,
                              "place_id": "provider-only-id"
                            },
                            {"formatted": "Missing latitude", "lon": 106.7},
                            {"formatted": "Out of range", "lat": 91, "lon": 106.7},
                            {"formatted": "   ", "lat": 10.7, "lon": 106.7}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<LocationSearchResponse> results = client.search("Dinh Độc Lập");

        assertThat(results)
                .containsExactly(new LocationSearchResponse(
                        "Dinh Độc Lập, Quận 1, Thành phố Hồ Chí Minh, Việt Nam",
                        new BigDecimal("10.7770754"),
                        new BigDecimal("106.6952941")));
        server.verify();
    }

    @Test
    void returnsEmptyListForEmptyProviderResults() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeoapifyGeocodingClient client = new GeoapifyGeocodingClient(builder, "https://geoapify.test", API_KEY);

        server.expect(requestTo(containsString("/v1/geocode/search?")))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.search("Landmark 81")).isEmpty();
        server.verify();
    }

    @Test
    void translatesMalformedResponseToBoundaryException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeoapifyGeocodingClient client = new GeoapifyGeocodingClient(builder, "https://geoapify.test", API_KEY);

        server.expect(requestTo(containsString("/v1/geocode/search?")))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("Landmark 81"))
                .isInstanceOf(GeocodingUnavailableException.class)
                .hasMessage("Location search provider is unavailable");
        server.verify();
    }

    @Test
    void translatesProviderClientFailureToBoundaryException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeoapifyGeocodingClient client = new GeoapifyGeocodingClient(builder, "https://geoapify.test", API_KEY);

        server.expect(requestTo(containsString("/v1/geocode/search?"))).andRespond(withBadRequest());

        assertThatThrownBy(() -> client.search("Landmark 81"))
                .isInstanceOf(GeocodingUnavailableException.class)
                .hasMessage("Location search provider is unavailable");
        server.verify();
    }

    @Test
    void translatesProviderServerFailureWithoutLeakingApiKey() {
        String sensitiveApiKey = "secret-key-that-must-not-leak";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeoapifyGeocodingClient client = new GeoapifyGeocodingClient(builder, "https://geoapify.test", sensitiveApiKey);

        server.expect(requestTo(containsString("/v1/geocode/search?"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.search("Landmark 81"))
                .isInstanceOf(GeocodingUnavailableException.class)
                .hasMessage("Location search provider is unavailable")
                .hasMessageNotContaining(sensitiveApiKey)
                .hasNoCause();
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeoapifyGeocodingClient client = new GeoapifyGeocodingClient(builder, "https://geoapify.test", "   ");

        server.expect(never(), requestTo(containsString("/v1/geocode/search")));

        assertThatThrownBy(() -> client.search("Landmark 81"))
                .isInstanceOf(GeocodingUnavailableException.class)
                .hasMessage("Location search provider is not configured")
                .hasMessageNotContaining("apiKey");
        server.verify();
    }
}
