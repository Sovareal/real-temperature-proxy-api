package com.example.weatherproxy.api;

import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/weather")
@Validated
@Tag(name = "Weather", description = "Current weather conditions proxied from Open-Meteo")
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weather", description =
            "Returns current temperature and wind speed for the given coordinates. " +
            "Responses are cached for 60 seconds; the X-Cache header indicates whether " +
            "the result was served from cache (HIT) or fetched from Open-Meteo (MISS).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current weather conditions",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "X-Cache",
                            description = "HIT if served from cache, MISS if fetched from upstream",
                            schema = @Schema(type = "string", allowableValues = {"HIT", "MISS"})
                    )),
            @ApiResponse(responseCode = "400", description = "Invalid or missing coordinates",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "503", description = "Upstream unavailable or circuit breaker open",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "504", description = "Upstream timed out",
                    content = @Content(mediaType = "application/problem+json"))
    })
    public Mono<ResponseEntity<WeatherResponse>> getCurrent(
            @Parameter(description = "Latitude", example = "52.52")
            @RequestParam
            @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90.0")
            @DecimalMax(value = "90.0",   message = "Latitude must be <= 90.0")
            double lat,

            @Parameter(description = "Longitude", example = "13.41")
            @RequestParam
            @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
            @DecimalMax(value = "180.0",  message = "Longitude must be <= 180.0")
            double lon
    ) {
        log.debug("Incoming request: lat={}, lon={}", lat, lon);
        return weatherService.getCurrentWeather(lat, lon)
                .map(result -> ResponseEntity.ok()
                        .header("X-Cache", result.cacheHit() ? "HIT" : "MISS")
                        .body(result.response()));
    }
}
