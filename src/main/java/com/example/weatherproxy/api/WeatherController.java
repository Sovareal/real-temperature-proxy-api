package com.example.weatherproxy.api;

import com.example.weatherproxy.api.dto.WeatherResponse;
import com.example.weatherproxy.service.WeatherService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/weather")
@Validated
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    public Mono<WeatherResponse> getCurrent(
            @RequestParam
            @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
            @DecimalMax(value = "90.0",  message = "Latitude must be <= 90.0")
            double lat,

            @RequestParam
            @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
            @DecimalMax(value = "180.0",  message = "Longitude must be <= 180.0")
            double lon
    ) {
        log.debug("Incoming request: lat={}, lon={}", lat, lon);
        return weatherService.getCurrentWeather(lat, lon);
    }
}
