package com.example.weatherproxy.support;

public final class OpenMeteoFixtures {

    private OpenMeteoFixtures() {}

    public static final String FORECAST_RESPONSE = """
            {
              "latitude": 52.52,
              "longitude": 13.41,
              "current": {
                "time": "2026-01-11T10:00",
                "interval": 900,
                "temperature_2m": 1.2,
                "wind_speed_10m": 9.7
              }
            }
            """;
}
