package com.example.weatherproxy.config;

import com.example.weatherproxy.api.dto.WeatherResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, WeatherResponse> weatherCache(WeatherProperties props, MeterRegistry meterRegistry) {
        WeatherProperties.CacheProperties cacheProps = props.cache();
        return Caffeine.newBuilder()
                .maximumSize(cacheProps.maxSize())
                .expireAfterWrite(Duration.ofSeconds(cacheProps.ttlSeconds()))
                .recordStats(() -> new CaffeineStatsCounter(meterRegistry, "weather_cache"))
                .build();
    }
}
