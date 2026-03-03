# Real Temperature Proxy API

REST API that proxies [Open-Meteo](https://open-meteo.com) to return current temperature and wind speed for any coordinate.

## Stack

- **Java 21** + Spring Boot 3.4.3 (WebFlux, reactive/non-blocking)
- **Caffeine** cache — 60s TTL, keyed by lat/lon
- **Resilience4j** circuit breaker with request coalescing (thundering herd prevention)
- **Micrometer + Prometheus + Grafana** — pre-built dashboard included
- **OpenAPI** docs at `/swagger-ui.html`

## API

```
GET /v1/weather/current?lat={lat}&lon={lon}
```

| Name | Range           |
|------|-----------------|
| lat  | −90.0 to 90.0   |
| lon  | −180.0 to 180.0 |

```json
{
  "location": { "lat": 52.52, "lon": 13.41 },
  "current": { "temperatureC": 1.2, "windSpeedKmh": 9.7 },
  "source": "open-meteo",
  "retrievedAt": "2026-01-11T10:12:54Z"
}
```

`X-Cache: HIT | MISS` — indicates whether the response was served from cache.

Error responses follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457): `400` invalid params, `503` upstream unavailable / circuit breaker open, `504` upstream timeout.

## Running locally

**Docker Compose** (includes Prometheus + Grafana):

```bash
docker compose up
curl "http://localhost:8080/v1/weather/current?lat=52.52&lon=13.41"
```

| Service    | URL                       |
|------------|---------------------------|
| API        | http://localhost:8080      |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090      |
| Grafana    | http://localhost:3000 (admin/admin) |

**Without Docker** (Java 21 required):

```bash
./gradlew bootRun   # runs with local profile — plain-text logs
```

## Tests

```bash
./gradlew test
```

26 tests: unit (cache, service, controller slice) + full integration tests with WireMock covering happy path, cache coalescing, 503, and 504 scenarios.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `WEATHER_UPSTREAM_BASE_URL` | https://api.open-meteo.com | Open-Meteo base URL |
| `WEATHER_UPSTREAM_TIMEOUT_MS` | 1000 | Per-request timeout (ms) |
| `WEATHER_UPSTREAM_CONNECT_TIMEOUT_MS` | 500 | TCP connect timeout (ms) |
| `WEATHER_UPSTREAM_MAX_CONNECTIONS` | 500 | HTTP connection pool size |
| `WEATHER_CACHE_TTL_SECONDS` | 60 | Cache TTL (s) |
| `WEATHER_CACHE_MAX_SIZE` | 10000 | Max cached coordinate pairs |
| `WEATHER_CACHE_COORDINATE_PRECISION` | 4 | Decimal places for cache key rounding |

Circuit breaker: 50% failure threshold, 30s open wait, 100-call sliding window — configured in `application.yml`.

## Kubernetes

```bash
# Build image
docker build -t weather-proxy:0.1.0 .

# Or pull from GHCR (built by CI on every master push)
docker pull ghcr.io/sovareal/real-temperature-proxy-api:latest

# Deploy (Deployment + Service + HPA + ConfigMap)
kubectl apply -f k8s/

# Verify and test
kubectl rollout status deployment/weather-proxy
kubectl port-forward svc/weather-proxy 8080:80
curl "http://localhost:8080/v1/weather/current?lat=52.52&lon=13.41"
```

HPA scales 2–10 replicas on CPU > 70% or memory > 768Mi. Graceful shutdown: 5s drain + 30s Spring shutdown within 35s termination period.

> **Docker Desktop:** metrics-server is not pre-installed — see the [metrics-server install guide](https://github.com/kubernetes-sigs/metrics-server) and add `--kubelet-insecure-tls` to its args.

## Observability

```
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/prometheus
GET /actuator/info
```

## CI/CD

GitHub Actions runs on every push and PR: tests → Docker build. On merge to `master` the image is pushed to `ghcr.io/sovareal/real-temperature-proxy-api` tagged with `latest` and the commit SHA.
