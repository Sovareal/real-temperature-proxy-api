package com.example.weatherproxy.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoCurrentDto(
        @JsonProperty("temperature_2m")  double temperature2m,
        @JsonProperty("wind_speed_10m")  double windSpeed10m
) {}
