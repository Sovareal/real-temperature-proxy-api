package com.example.weatherproxy.client;

import com.example.weatherproxy.api.exception.UpstreamTimeoutException;
import com.example.weatherproxy.api.exception.UpstreamUnavailableException;
import com.example.weatherproxy.config.WeatherProperties;
import com.example.weatherproxy.config.WebClientConfig;
import com.example.weatherproxy.support.OpenMeteoFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(
        classes = {OpenMeteoClientTest.TestConfig.class, WebClientConfig.class, OpenMeteoClient.class},
        properties = {
                "weather.upstream.base-url=${wiremock.server.baseUrl}",
                "weather.upstream.timeout-ms=500",
                "weather.upstream.connect-timeout-ms=200",
                "weather.upstream.max-connections=10",
                "weather.cache.ttl-seconds=60",
                "weather.cache.max-size=100",
                "weather.cache.coordinate-precision=4"
        }
)
@EnableConfigurationProperties(WeatherProperties.class)
@EnableWireMock(@ConfigureWireMock(name = "open-meteo"))
class OpenMeteoClientTest {

    private static final String FORECAST_RESPONSE = OpenMeteoFixtures.FORECAST_RESPONSE;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    private OpenMeteoClient client;

    @InjectWireMock("open-meteo")
    private com.github.tomakehurst.wiremock.WireMockServer wireMock;

    @Test
    void returnsCurrentConditionsOnSuccess() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FORECAST_RESPONSE)));

        StepVerifier.create(client.fetchCurrent(52.52, 13.41))
                .assertNext(response -> {
                    assert response.current().temperature2m() == 1.2;
                    assert response.current().windSpeed10m() == 9.7;
                })
                .verifyComplete();
    }

    @Test
    void throwsUpstreamUnavailableOnServerError() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse().withStatus(500)));

        StepVerifier.create(client.fetchCurrent(52.52, 13.41))
                .expectError(UpstreamUnavailableException.class)
                .verify();
    }

    @Test
    void throwsUpstreamTimeoutOnDelay() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(600)  // exceeds timeout-ms=500
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FORECAST_RESPONSE)));

        StepVerifier.create(client.fetchCurrent(52.52, 13.41))
                .expectError(UpstreamTimeoutException.class)
                .verify();
    }
}
