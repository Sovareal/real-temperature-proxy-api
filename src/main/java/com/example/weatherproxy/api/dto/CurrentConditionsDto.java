package com.example.weatherproxy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current weather measurements")
public record CurrentConditionsDto(
        @Schema(description = "Air temperature at 2 m above ground", example = "1.2") double temperatureC,
        @Schema(description = "Wind speed at 10 m above ground", example = "9.7") double windSpeedKmh
) {}
