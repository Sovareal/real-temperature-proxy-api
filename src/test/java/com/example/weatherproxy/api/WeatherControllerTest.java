package com.example.weatherproxy.api;

import com.example.weatherproxy.api.dto.CurrentConditionsDto;
import com.example.weatherproxy.api.dto.LocationDto;
import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.api.exception.UpstreamTimeoutException;
import com.example.weatherproxy.api.exception.UpstreamUnavailableException;
import com.example.weatherproxy.service.WeatherResult;
import com.example.weatherproxy.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@WebFluxTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void returnsWeatherResponseOnValidRequest() {
        WeatherResponse response = new WeatherResponse(
                new LocationDto(52.52, 13.41),
                new CurrentConditionsDto(1.2, 9.7),
                "open-meteo",
                Instant.parse("2026-01-11T10:12:54Z")
        );
        when(weatherService.getCurrentWeather(52.52, 13.41))
                .thenReturn(Mono.just(new WeatherResult(response, false)));

        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=13.41")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectHeader().valueEquals("X-Cache", "MISS")
                .expectBody()
                .jsonPath("$.location.lat").isEqualTo(52.52)
                .jsonPath("$.location.lon").isEqualTo(13.41)
                .jsonPath("$.current.temperatureC").isEqualTo(1.2)
                .jsonPath("$.current.windSpeedKmh").isEqualTo(9.7)
                .jsonPath("$.source").isEqualTo("open-meteo")
                .jsonPath("$.retrievedAt").isEqualTo("2026-01-11T10:12:54Z");
    }

    @Test
    void returnsCacheHitHeaderWhenServedFromCache() {
        WeatherResponse response = new WeatherResponse(
                new LocationDto(52.52, 13.41),
                new CurrentConditionsDto(1.2, 9.7),
                "open-meteo",
                Instant.parse("2026-01-11T10:12:54Z")
        );
        when(weatherService.getCurrentWeather(52.52, 13.41))
                .thenReturn(Mono.just(new WeatherResult(response, true)));

        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=13.41")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Cache", "HIT");
    }

    @Test
    void returns400WhenLatIsMissing() {
        webTestClient.get()
                .uri("/v1/weather/current?lon=13.41")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void returns400WhenLatIsOutOfRange() {
        webTestClient.get()
                .uri("/v1/weather/current?lat=91.0&lon=13.41")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Invalid Coordinates");
    }

    @Test
    void returns400WhenLonIsOutOfRange() {
        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=181.0")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void returns504OnUpstreamTimeout() {
        when(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(Mono.error(new UpstreamTimeoutException("timed out")));

        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=13.41")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.status").isEqualTo(504)
                .jsonPath("$.title").isEqualTo("Upstream Timeout");
    }

    @Test
    void returns503OnUpstreamUnavailable() {
        when(weatherService.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(Mono.error(new UpstreamUnavailableException("circuit open")));

        webTestClient.get()
                .uri("/v1/weather/current?lat=52.52&lon=13.41")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.title").isEqualTo("Upstream Unavailable");
    }
}
