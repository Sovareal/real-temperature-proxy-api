package com.example.weatherproxy.client.dto;

// Top-level response from GET /v1/forecast?current=temperature_2m,wind_speed_10m
// Note: upstream also returns current.time as a local ISO-8601 string (no timezone).
// We ignore it and use Instant.now() for retrievedAt.
public record OpenMeteoResponse(OpenMeteoCurrentDto current) {}
