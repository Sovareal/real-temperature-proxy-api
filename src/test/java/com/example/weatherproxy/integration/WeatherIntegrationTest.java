package com.example.weatherproxy.integration;

import com.example.weatherproxy.WeatherProxyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import com.example.weatherproxy.support.OpenMeteoFixtures;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(
        classes = WeatherProxyApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "weather.upstream.base-url=${wiremock.server.baseUrl}",
                "weather.upstream.timeout-ms=1000",
                "weather.cache.ttl-seconds=60"
        }
)
@EnableWireMock(@ConfigureWireMock(name = "open-meteo"))
class WeatherIntegrationTest {

    private static final String FORECAST_RESPONSE = OpenMeteoFixtures.FORECAST_RESPONSE;

    @Autowired
    private WebTestClient webTestClient;

    @InjectWireMock("open-meteo")
    private com.github.tomakehurst.wiremock.WireMockServer wireMock;

    @Test
    void fullRequestReturnsCorrectResponseShape() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FORECAST_RESPONSE)));

        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=13.41")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.location.lat").isEqualTo(52.52)
                .jsonPath("$.location.lon").isEqualTo(13.41)
                .jsonPath("$.current.temperatureC").isEqualTo(1.2)
                .jsonPath("$.current.windSpeedKmh").isEqualTo(9.7)
                .jsonPath("$.source").isEqualTo("open-meteo")
                .jsonPath("$.retrievedAt").exists();
    }

    @Test
    void secondIdenticalRequestHitsCacheAndCallsUpstreamOnce() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FORECAST_RESPONSE)));

        // First request -- cache miss, calls upstream
        webTestClient.get()
                .uri("/v1/weather/current?lat=10.0&lon=20.0")
                .exchange()
                .expectStatus().isOk();

        // Second request with same coordinates -- should hit cache
        webTestClient.get()
                .uri("/v1/weather/current?lat=10.0&lon=20.0")
                .exchange()
                .expectStatus().isOk();

        // Upstream called exactly once despite two requests
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v1/forecast"))
                .withQueryParam("latitude", equalTo("10.0"))
                .withQueryParam("longitude", equalTo("20.0")));
    }

    @Test
    void responseIncludesRequestIdHeader() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FORECAST_RESPONSE)));

        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=13.41")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Request-Id");
    }

    @Test
    void returns400ForInvalidCoordinates() {
        webTestClient.get()
                .uri("/v1/weather/current?lat=999&lon=13.41")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }
}
