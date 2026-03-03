package com.example.weatherproxy.api.dto;

import java.time.Instant;

public record WeatherResponse(
        LocationDto location,
        CurrentConditionsDto current,
        String source,
        Instant retrievedAt
) {}
