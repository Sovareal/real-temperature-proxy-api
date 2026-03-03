package com.example.weatherproxy.client;

import com.example.weatherproxy.api.exception.UpstreamTimeoutException;
import com.example.weatherproxy.api.exception.UpstreamUnavailableException;
import com.example.weatherproxy.client.dto.OpenMeteoResponse;
import com.example.weatherproxy.config.WeatherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private static final String CURRENT_VARIABLES = "temperature_2m,wind_speed_10m";

    private final WebClient webClient;
    private final Duration timeout;

    public OpenMeteoClient(WebClient openMeteoWebClient, WeatherProperties props) {
        this.webClient = openMeteoWebClient;
        this.timeout = Duration.ofMillis(props.upstream().timeoutMs());
    }

    public Mono<OpenMeteoResponse> fetchCurrent(double lat, double lon) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("current", CURRENT_VARIABLES)
                        .build())
                .retrieve()
                .bodyToMono(OpenMeteoResponse.class)
                .timeout(timeout)
                .doOnSuccess(r -> log.debug("Upstream response received: lat={}, lon={}", lat, lon))
                .onErrorMap(TimeoutException.class, ex ->
                        new UpstreamTimeoutException("Open-Meteo timed out after %dms".formatted(timeout.toMillis()), ex))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.warn("Upstream HTTP error {}: lat={}, lon={}", ex.getStatusCode(), lat, lon);
                    return new UpstreamUnavailableException(
                            "Open-Meteo returned " + ex.getStatusCode(), ex);
                })
                .onErrorMap(ex -> !(ex instanceof UpstreamTimeoutException
                        || ex instanceof UpstreamUnavailableException),
                        ex -> new UpstreamUnavailableException("Upstream connection failed", ex));
    }
}
