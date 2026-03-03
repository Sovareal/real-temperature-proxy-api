package com.example.weatherproxy.cache;

import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.config.WeatherProperties;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class WeatherCacheService {

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheService.class);

    private final Cache<String, WeatherResponse> cache;
    private final int coordinatePrecision;

    public WeatherCacheService(Cache<String, WeatherResponse> weatherCache, WeatherProperties props) {
        this.cache = weatherCache;
        this.coordinatePrecision = props.cache().coordinatePrecision();
    }

    public Optional<WeatherResponse> get(double lat, double lon) {
        String key = buildKey(lat, lon);
        WeatherResponse cached = cache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit: key={}", key);
        } else {
            log.debug("Cache miss: key={}", key);
        }
        return Optional.ofNullable(cached);
    }

    public void put(double lat, double lon, WeatherResponse response) {
        cache.put(buildKey(lat, lon), response);
    }

    public String buildKey(double lat, double lon) {
        String roundedLat = BigDecimal.valueOf(lat).setScale(coordinatePrecision, RoundingMode.HALF_UP).toPlainString();
        String roundedLon = BigDecimal.valueOf(lon).setScale(coordinatePrecision, RoundingMode.HALF_UP).toPlainString();
        return roundedLat + "," + roundedLon;
    }
}
