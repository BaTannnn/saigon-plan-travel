package com.saigonplantravel.backend.location.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.saigonplantravel.backend.location.dto.LocationSearchResponse;
import com.saigonplantravel.backend.location.exception.GeocodingUnavailableException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NominatimGeocodingClientTest {

    @Test
    void sendsPolicyCompliantScopedSearchAndMapsOnlyOwnedContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NominatimGeocodingClient client =
                new NominatimGeocodingClient(builder, "https://nominatim.test", "SaigonPlanTravel/1.0", Duration.ZERO);

        server.expect(once(), requestTo(containsString("/search?")))
                .andExpect(requestTo(not(containsString("%2520"))))
                .andExpect(header("User-Agent", "SaigonPlanTravel/1.0"))
                .andExpect(queryParam("q", "Dinh%20%C4%90%E1%BB%99c%20L%E1%BA%ADp"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("limit", "5"))
                .andExpect(queryParam("countrycodes", "vn"))
                .andExpect(queryParam("viewbox", "106.35,11.20,107.10,10.35"))
                .andExpect(queryParam("bounded", "0"))
                .andExpect(queryParam("accept-language", "vi"))
                .andRespond(withSuccess(
                        """
                        [
                          {
                            "place_id": 123,
                            "display_name": "Dinh Độc Lập, Quận 1, Thành phố Hồ Chí Minh, Việt Nam",
                            "lat": "10.7770754",
                            "lon": "106.6952941",
                            "type": "museum"
                          },
                          {"display_name": "Broken", "lat": null, "lon": "106.7"}
                        ]
                        """,
                        MediaType.APPLICATION_JSON));

        List<LocationSearchResponse> results = client.search("Dinh Độc Lập");

        assertThat(results)
                .containsExactly(new LocationSearchResponse(
                        "Dinh Độc Lập, Quận 1, Thành phố Hồ Chí Minh, Việt Nam",
                        new java.math.BigDecimal("10.7770754"),
                        new java.math.BigDecimal("106.6952941")));
        server.verify();
    }

    @Test
    void translatesProviderFailureToBoundaryException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NominatimGeocodingClient client =
                new NominatimGeocodingClient(builder, "https://nominatim.test", "SaigonPlanTravel/1.0", Duration.ZERO);

        server.expect(requestTo(containsString("/search?"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.search("Dinh Độc Lập"))
                .isInstanceOf(GeocodingUnavailableException.class)
                .hasMessage("Location search provider is unavailable");
        server.verify();
    }
}
