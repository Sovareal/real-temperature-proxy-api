package com.example.weatherproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather")
public record WeatherProperties(
        UpstreamProperties upstream,
        CacheProperties cache,
        ResilienceProperties resilience
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

    public record ResilienceProperties(CircuitBreakerProperties circuitBreaker) {
        public record CircuitBreakerProperties(
                float failureRateThreshold,
                long slowCallDurationThresholdMs,
                float slowCallRateThreshold,
                int permittedCallsInHalfOpen,
                int slidingWindowSize,
                long waitDurationInOpenStateMs
        ) {}
    }
}
