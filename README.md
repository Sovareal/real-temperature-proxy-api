# Real Temperature Proxy API

REST API that proxies [Open-Meteo](https://open-meteo.com) to return current temperature and wind speed for any coordinate.

## Stack

- **Java 21** + Spring Boot 3.4.3 (WebFlux, reactive/non-blocking)
- **Caffeine** cache — 60s TTL, keyed by lat/lon rounded to 4 decimal places
- **Resilience4j** circuit breaker — protects against Open-Meteo outages
- **Micrometer + Prometheus** metrics with histogram support
- **Grafana** dashboard — 7 pre-built panels

## API

```
GET /v1/weather/current?lat={lat}&lon={lon}
```

**Parameters**

| Name | Type  | Required | Range           |
|------|-------|----------|-----------------|
| lat  | float | yes      | -90.0 to 90.0   |
| lon  | float | yes      | -180.0 to 180.0 |

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

| Status | Condition |
|--------|-----------|
| 400    | Missing or invalid `lat`/`lon` parameters |
| 503    | Circuit breaker open — upstream unavailable |
| 504    | Open-Meteo did not respond within 1s |

## Running locally

### Docker Compose (recommended)

Starts the app alongside Prometheus and Grafana with a pre-built dashboard.

**Prerequisites:** Docker

```bash
docker compose up
```

| Service    | URL                                          | Notes              |
|------------|----------------------------------------------|--------------------|
| API        | http://localhost:8080                        |                    |
| Prometheus | http://localhost:9090                        |                    |
| Grafana    | http://localhost:3000                        | login: admin/admin |

```bash
# Test the API
curl "http://localhost:8080/v1/weather/current?lat=52.52&lon=13.41"

# Verify second request is served from cache (check X-Request-Id differs but response is identical)
curl "http://localhost:8080/v1/weather/current?lat=52.52&lon=13.41"
```

The Grafana dashboard (**Weather Proxy** at http://localhost:3000) includes:

| Panel | What it shows |
|-------|---------------|
| Request Rate (RPS) | Requests per second |
| Error Rate (%) | 4xx + 5xx percentage |
| Response Latency | p50 / p95 / p99 end-to-end |
| Cache Hit Ratio | Caffeine cache hit % |
| Upstream Latency | p50 / p95 / p99 to Open-Meteo |
| Circuit Breaker State | closed / open / half_open |
| JVM Heap Used | Heap used vs max |

### Without Docker

**Prerequisites:** Java 21

```bash
./gradlew bootRun
```

Runs with the `local` Spring profile — plain-text logs, no JSON formatting.

## Tests

```bash
./gradlew test
```

Covers unit tests (cache, service, controller) and a full integration test with WireMock standing in for Open-Meteo. The integration test verifies that two identical requests result in exactly one upstream call.

## Configuration

All settings can be overridden via environment variables:

| Variable                              | Default                      | Description                        |
|---------------------------------------|------------------------------|------------------------------------|
| `WEATHER_UPSTREAM_BASE_URL`           | https://api.open-meteo.com   | Open-Meteo base URL                |
| `WEATHER_UPSTREAM_TIMEOUT_MS`         | 1000                         | Per-request timeout (ms)           |
| `WEATHER_UPSTREAM_CONNECT_TIMEOUT_MS` | 500                          | TCP connect timeout (ms)           |
| `WEATHER_UPSTREAM_MAX_CONNECTIONS`    | 500                          | Max HTTP connection pool size      |
| `WEATHER_CACHE_TTL_SECONDS`           | 60                           | Cache entry lifetime (s)           |
| `WEATHER_CACHE_MAX_SIZE`              | 10000                        | Max cached coordinate pairs        |
| `WEATHER_CACHE_COORDINATE_PRECISION`  | 4                            | Decimal places for cache key       |

Circuit breaker behaviour is configured under `resilience4j.circuitbreaker.instances.open-meteo` in `application.yml` (failure threshold 50%, 30s open wait, sliding window 20 calls).

## Health checks

```
GET /actuator/health/liveness    # JVM alive
GET /actuator/health/readiness   # Ready to serve traffic
GET /actuator/prometheus         # Prometheus scrape endpoint
```

## Kubernetes

### Prerequisites

- `kubectl` configured against a cluster
- The app Docker image built and available to the cluster

### Build and tag the image

```bash
./gradlew bootJar
docker build -t weather-proxy:0.1.0 .
```

For a remote cluster, push to your registry and update the `image` field in `k8s/deployment.yaml`:

```bash
docker tag weather-proxy:0.1.0 <your-registry>/weather-proxy:0.1.0
docker push <your-registry>/weather-proxy:0.1.0
```

### Deploy

```bash
kubectl apply -f k8s/
```

Applies four resources:

| Resource | Details |
|----------|---------|
| `ConfigMap` | Runtime environment variables |
| `Deployment` | 2 initial replicas, resource limits (250m–1000m CPU, 512Mi–1Gi RAM) |
| `Service` | ClusterIP on port 80 |
| `HorizontalPodAutoscaler` | 2–10 replicas; scales on CPU > 70% or memory > 768Mi |

### Verify

```bash
# Watch rollout
kubectl rollout status deployment/weather-proxy

# Check pods and HPA
kubectl get pods,hpa -l app=weather-proxy

# Test endpoint (no ingress needed locally)
kubectl port-forward svc/weather-proxy 8080:80
curl "http://localhost:8080/v1/weather/current?lat=52.52&lon=13.41"
```

### HPA scaling behaviour

| Direction | Stabilisation window | Max change per period |
|-----------|---------------------|-----------------------|
| Scale up  | 30s                 | +2 pods / 60s         |
| Scale down| 300s                | −1 pod / 120s         |

> **Note — Docker Desktop:** metrics-server is not pre-installed. Install it once with:
> ```bash
> kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
> kubectl patch deployment metrics-server -n kube-system \
>   --type=json \
>   -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
> ```
> Managed clusters (EKS, GKE, AKS) ship metrics-server pre-installed.

### Graceful shutdown

Pods have a `preStop` sleep of 5s to allow the load balancer to drain connections, followed by a 30s Spring graceful shutdown, within a 35s `terminationGracePeriodSeconds`.
