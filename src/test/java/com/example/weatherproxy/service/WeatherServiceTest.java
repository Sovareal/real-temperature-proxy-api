package com.example.weatherproxy.service;

import com.example.weatherproxy.api.dto.CurrentConditionsDto;
import com.example.weatherproxy.api.dto.LocationDto;
import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.api.exception.UpstreamUnavailableException;
import com.example.weatherproxy.cache.WeatherCacheService;
import com.example.weatherproxy.client.OpenMeteoClient;
import com.example.weatherproxy.client.dto.OpenMeteoCurrentDto;
import com.example.weatherproxy.client.dto.OpenMeteoResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private OpenMeteoClient client;

    @Mock
    private WeatherCacheService cacheService;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheService.buildKey(anyDouble(), anyDouble())).thenReturn("52.5200,13.4100");
        weatherService = new WeatherService(
                client,
                cacheService,
                CircuitBreakerRegistry.ofDefaults(),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void returnsCachedResponseWithoutCallingUpstream() {
        WeatherResponse cached = sampleResponse(1.0, 2.0);
        when(cacheService.get(52.52, 13.41)).thenReturn(Optional.of(cached));

        StepVerifier.create(weatherService.getCurrentWeather(52.52, 13.41))
                .assertNext(result -> {
                    assertThat(result.response()).isEqualTo(cached);
                    assertThat(result.cacheHit()).isTrue();
                })
                .verifyComplete();

        verifyNoInteractions(client);
    }

    @Test
    void fetchesFromUpstreamOnCacheMiss() {
        when(cacheService.get(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(client.fetchCurrent(52.52, 13.41))
                .thenReturn(Mono.just(new OpenMeteoResponse(new OpenMeteoCurrentDto(12.5, 18.3))));

        StepVerifier.create(weatherService.getCurrentWeather(52.52, 13.41))
                .assertNext(result -> {
                    assertThat(result.cacheHit()).isFalse();
                    assertThat(result.response().current().temperatureC()).isEqualTo(12.5);
                    assertThat(result.response().current().windSpeedKmh()).isEqualTo(18.3);
                    assertThat(result.response().source()).isEqualTo("open-meteo");
                    assertThat(result.response().location().lat()).isEqualTo(52.52);
                })
                .verifyComplete();

        verify(cacheService).put(eq(52.52), eq(13.41), any());
    }

    @Test
    void propagatesUpstreamExceptionOnClientFailure() {
        when(cacheService.get(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(client.fetchCurrent(anyDouble(), anyDouble()))
                .thenReturn(Mono.error(new UpstreamUnavailableException("upstream down")));

        StepVerifier.create(weatherService.getCurrentWeather(52.52, 13.41))
                .expectError(UpstreamUnavailableException.class)
                .verify();

        verify(cacheService, never()).put(anyDouble(), anyDouble(), any());
    }

    @Test
    void throwsUpstreamUnavailableWhenCurrentDataIsNull() {
        when(cacheService.get(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(client.fetchCurrent(anyDouble(), anyDouble()))
                .thenReturn(Mono.just(new OpenMeteoResponse(null)));

        StepVerifier.create(weatherService.getCurrentWeather(52.52, 13.41))
                .expectErrorMatches(ex ->
                        ex instanceof UpstreamUnavailableException &&
                        ex.getMessage().contains("no current weather data"))
                .verify();
    }

    private WeatherResponse sampleResponse(double lat, double lon) {
        return new WeatherResponse(
                new LocationDto(lat, lon),
                new CurrentConditionsDto(12.5, 18.3),
                "open-meteo",
                Instant.now()
        );
    }
}
