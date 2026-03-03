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
                .expectNext(cached)
                .verifyComplete();

        verifyNoInteractions(client);
    }

    @Test
    void fetchesFromUpstreamOnCacheMiss() {
        when(cacheService.get(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(client.fetchCurrent(52.52, 13.41))
                .thenReturn(Mono.just(new OpenMeteoResponse(new OpenMeteoCurrentDto(12.5, 18.3))));

        StepVerifier.create(weatherService.getCurrentWeather(52.52, 13.41))
                .assertNext(response -> {
                    assert response.current().temperatureC() == 12.5;
                    assert response.current().windSpeedKmh() == 18.3;
                    assert response.source().equals("open-meteo");
                    assert response.location().lat() == 52.52;
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

    private WeatherResponse sampleResponse(double lat, double lon) {
        return new WeatherResponse(
                new LocationDto(lat, lon),
                new CurrentConditionsDto(12.5, 18.3),
                "open-meteo",
                Instant.now()
        );
    }
}
