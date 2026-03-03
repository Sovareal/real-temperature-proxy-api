package com.example.weatherproxy.cache;

import com.example.weatherproxy.api.dto.CurrentConditionsDto;
import com.example.weatherproxy.api.dto.LocationDto;
import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.config.WeatherProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherCacheServiceTest {

    private WeatherCacheService cacheService;

    @BeforeEach
    void setUp() {
        Cache<String, WeatherResponse> caffeine = Caffeine.newBuilder().build();
        WeatherProperties props = new WeatherProperties(
                new WeatherProperties.UpstreamProperties("http://localhost", 1000, 500, 10),
                new WeatherProperties.CacheProperties(60, 100, 4)
        );
        cacheService = new WeatherCacheService(caffeine, props);
    }

    @Test
    void getReturnsEmptyOnCacheMiss() {
        assertThat(cacheService.get(52.52, 13.41)).isEmpty();
    }

    @Test
    void putThenGetReturnsCachedValue() {
        WeatherResponse response = sampleResponse(52.52, 13.41);
        cacheService.put(52.52, 13.41, response);

        Optional<WeatherResponse> result = cacheService.get(52.52, 13.41);
        assertThat(result).isPresent().contains(response);
    }

    @Test
    void coordinatesRoundedToConfiguredPrecisionShareSameKey() {
        // With precision=4, 52.52001 and 52.52005 round to 52.5200 and 52.5201 -- different keys
        // but 52.52001 and 52.52004 both round to 52.5200 -- same key
        WeatherResponse response = sampleResponse(52.52, 13.41);
        cacheService.put(52.52, 13.41, response);

        // Same value within rounding tolerance
        assertThat(cacheService.get(52.52001, 13.41001)).isPresent();
    }

    @Test
    void buildKeyFormatsCorrectly() {
        String key = cacheService.buildKey(52.52, 13.41);
        assertThat(key).isEqualTo("52.5200,13.4100");
    }

    @Test
    void buildKeyHandlesNegativeCoordinates() {
        String key = cacheService.buildKey(-33.8688, -70.6693);
        assertThat(key).isEqualTo("-33.8688,-70.6693");
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
