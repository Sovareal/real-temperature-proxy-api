# Real Temperature Proxy API

REST API that proxies [Open-Meteo](https://open-meteo.com) to return current temperature and win speed for any coordinate.

## Stack

- Java 25 + Spring Boot 3.4.x (WebFlux, reactive)
- Caffeine cache (60s TTL, keyed by coordinates)
- Resilience4j circuit breaker
- Micrometer + Prometheus metrics

## API

```
GET /v1/weather/current?lat={lat}&lon={lon}
```

**Parameters**

| Name | Type  | Required | Range               |
|------|-------|----------|---------------------|
| lat  | float | yes      | -90.0 to 90.0       |
| lon  | float | yes      | -180.0 to 180.0     |

**Response**

```json
{
  "location": { "lat": 52.52, "lon": 13.41 },
  "current": {
    "temperatureC": 1.2,
    "windSpeedKmh": 9.7
  },
  "source": "open-meteo",
  "retrievedAt": "2026-01-11T10:12:54Z"
}
```

**Error responses** follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457).

## Running locally

**Prerequisites:** Java 25, Docker

```bash
# Start app + Prometheus + Grafana
docker compose up

# API
curl "http://localhost:8080/v1/weather/current?lat=52.52&lon=13.41"

# Metrics
open http://localhost:3000   # Grafana (admin/admin)
```

**Without Docker:**

```bash
./gradlew bootRun
```

## Health checks

```
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/prometheus
```

## Configuration

Key settings via environment variables (all have defaults):

| Variable                        | Default | Description                  |
|---------------------------------|---------|------------------------------|
| `WEATHER_UPSTREAM_TIMEOUT_MS`   | 1000    | Open-Meteo call timeout (ms) |
| `WEATHER_CACHE_TTL_SECONDS`     | 60      | Cache entry lifetime         |
| `WEATHER_CACHE_MAX_SIZE`        | 10000   | Max cached coordinate pairs  |

## Kubernetes

```bash
kubectl apply -f k8s/
```

Includes Deployment (2-10 replicas via HPA), Service, and ConfigMap.
