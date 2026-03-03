package com.example.weatherproxy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Geographic coordinates")
public record LocationDto(
        @Schema(description = "Latitude (-90 to 90)", example = "52.52") double lat,
        @Schema(description = "Longitude (-180 to 180)", example = "13.41") double lon
) {}
