package com.example.weatherproxy.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    public static final String OPEN_METEO_CB = "open-meteo";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(WeatherProperties props) {
        WeatherProperties.ResilienceProperties.CircuitBreakerProperties cb =
                props.resilience().circuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(cb.slowCallDurationThresholdMs()))
                .slowCallRateThreshold(cb.slowCallRateThreshold())
                .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpen())
                .slidingWindowSize(cb.slidingWindowSize())
                .waitDurationInOpenState(Duration.ofMillis(cb.waitDurationInOpenStateMs()))
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
