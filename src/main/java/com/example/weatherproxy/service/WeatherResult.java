package com.example.weatherproxy.service;

import com.example.weatherproxy.api.dto.WeatherResponse;

public record WeatherResult(WeatherResponse response, boolean cacheHit) {}
