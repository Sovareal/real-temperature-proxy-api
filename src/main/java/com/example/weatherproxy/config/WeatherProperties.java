package com.example.weatherproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather")
public record WeatherProperties(
        UpstreamProperties upstream,
        CacheProperties cache
) {
    public record UpstreamProperties(
            String baseUrl,
            long timeoutMs,
            long connectTimeoutMs,
            int maxConnections
    ) {}

    public record CacheProperties(
            long ttlSeconds,
            long maxSize,
            int coordinatePrecision
    ) {}
}
