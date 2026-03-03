package com.example.weatherproxy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Current weather conditions for the requested location")
public record WeatherResponse(
        @Schema(description = "Requested coordinates") LocationDto location,
        @Schema(description = "Current weather measurements") CurrentConditionsDto current,
        @Schema(description = "Data provider", example = "open-meteo") String source,
        @Schema(description = "UTC timestamp when the data was retrieved", example = "2026-01-11T10:12:54Z")
        Instant retrievedAt
) {}
