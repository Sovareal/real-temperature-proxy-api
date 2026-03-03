package com.example.weatherproxy.service;

import com.example.weatherproxy.api.dto.CurrentConditionsDto;
import com.example.weatherproxy.api.dto.LocationDto;
import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.api.exception.UpstreamUnavailableException;
import com.example.weatherproxy.cache.WeatherCacheService;
import com.example.weatherproxy.client.OpenMeteoClient;
import com.example.weatherproxy.client.dto.OpenMeteoResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final String SOURCE = "open-meteo";

    private final OpenMeteoClient client;
    private final WeatherCacheService cache;
    private final CircuitBreaker circuitBreaker;
    private final Timer upstreamTimer;

    private final ConcurrentHashMap<String, Mono<WeatherResponse>> inFlight = new ConcurrentHashMap<>();

    public WeatherService(
            OpenMeteoClient client,
            WeatherCacheService cache,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry
    ) {
        this.client = client;
        this.cache = cache;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(SOURCE);
        this.upstreamTimer = Timer.builder("weather.upstream.duration")
                .description("Open-Meteo upstream call latency")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    public Mono<WeatherResult> getCurrentWeather(double lat, double lon) {
        return cache.get(lat, lon)
                .map(cached -> Mono.just(new WeatherResult(cached, true)))
                .orElseGet(() -> coalesced(lat, lon).map(r -> new WeatherResult(r, false)));
    }

    private Mono<WeatherResponse> coalesced(double lat, double lon) {
        String key = cache.buildKey(lat, lon);
        return inFlight.computeIfAbsent(key, k ->
                fetchFromUpstream(lat, lon)
                        .doFinally(signal -> inFlight.remove(key))
                        .cache());
    }

    private Mono<WeatherResponse> fetchFromUpstream(double lat, double lon) {
        Timer.Sample sample = Timer.start();
        return client.fetchCurrent(lat, lon)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .map(response -> toWeatherResponse(response, lat, lon))
                .doOnSuccess(response -> {
                    sample.stop(upstreamTimer);
                    cache.put(lat, lon, response);
                    log.info("Upstream fetch success: lat={}, lon={}, tempC={}",
                            lat, lon, response.current().temperatureC());
                })
                .doOnError(ex -> {
                    sample.stop(upstreamTimer);
                    log.warn("Upstream fetch failed: lat={}, lon={}, error={}", lat, lon, ex.getMessage());
                })
                .onErrorMap(CallNotPermittedException.class, ex ->
                        new UpstreamUnavailableException("Circuit breaker open -- upstream calls suspended", ex));
    }

    private WeatherResponse toWeatherResponse(OpenMeteoResponse upstream, double lat, double lon) {
        if (upstream.current() == null) {
            throw new UpstreamUnavailableException("Open-Meteo returned no current weather data");
        }
        return new WeatherResponse(
                new LocationDto(lat, lon),
                new CurrentConditionsDto(
                        upstream.current().temperature2m(),
                        upstream.current().windSpeed10m()
                ),
                SOURCE,
                Instant.now()
        );
    }
}
