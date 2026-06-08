# ShopFlow

> Event-driven microservices e-commerce backend implementing the **Saga choreography pattern** for distributed transactions — built with Spring Boot 3.2, RabbitMQ, PostgreSQL, and a full Grafana observability stack.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Option A — Full Docker (recommended)](#option-a--full-docker-recommended)
    - [Option B — Local Dev (services on host)](#option-b--local-dev-services-on-host)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [The Saga Pattern](#the-saga-pattern)
- [Observability](#observability)
- [Testing](#testing)
- [Ports Reference](#ports-reference)
- [Roadmap](#roadmap)

---

## Architecture Overview

```
                        ┌─────────────────────────────────────────────────────┐
                        │                   Docker Network                     │
                        │                                                       │
  Client ──────────────▶│  API Gateway :8086                                   │
                        │      │  JWT validation (JwtAuthFilter)                │
                        │      │  Circuit Breaker (Resilience4j)                │
                        │      │                                                │
                        │   ┌──┴──────────────┬──────────────────┐             │
                        │   ▼                 ▼                  │             │
                        │  Order Service   Payment Service        │             │
                        │     :8081           :8082               │             │
                        │     │  PostgreSQL    │  PostgreSQL       │             │
                        │     │  (orderdb)     │  (paymentdb)      │             │
                        │     │                │                   │             │
                        │     └──────┬─────────┘                   │             │
                        │           ▼                              │             │
                        │       RabbitMQ                           │             │
                        │   saga.exchange                          │             │
                        │   ├── payment.request                    │             │
                        │   ├── payment.response                   │             │
                        │   └── order.events                       │             │
                        │                                          │             │
                        │  Prometheus · Loki · Tempo · Grafana     │             │
                        └─────────────────────────────────────────────────────┘
```

The system uses **Saga choreography** — services communicate entirely via RabbitMQ events with no direct HTTP calls between them. If payment fails, a compensation event automatically rolls the order back.

---

## Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| `api-gateway` | `8086` | — | JWT auth, routing, circuit breakers, CORS |
| `order-service` | `8081` | PostgreSQL `orderdb` | Order lifecycle, Saga orchestrator |
| `payment-service` | `8082` | PostgreSQL `paymentdb` | Payment processing, Saga participant |
| `frontend` | `3000` | — | Static UI served via Nginx |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| API Gateway | Spring Cloud Gateway (WebFlux/reactive) |
| Messaging | Spring AMQP + RabbitMQ 4.3 |
| Persistence | Spring Data JPA + PostgreSQL 15 |
| Auth | JWT (JJWT 0.12.5) |
| Resilience | Resilience4j circuit breaker |
| Metrics | Micrometer + Prometheus |
| Tracing | OpenTelemetry + Grafana Tempo |
| Logging | Logback + Loki4j + Grafana Loki |
| Dashboards | Grafana |
| Containers | Docker + Docker Compose |
| Build | Maven |

---

## Project Structure

```
shopflow-complete/
├── docker-compose.yml              ← Full stack (all services + infra + observability)
├── docker-compose.infra.yml        ← Infrastructure only (DBs + RabbitMQ)
│
├── api-gateway/
│   └── src/main/java/com/shopflow/gateway/
│       ├── config/
│       │   ├── JwtUtil.java            ← Validates JWT signature using shared secret
│       │   └── SecurityConfig.java     ← WebFlux security — permits all (JwtAuthFilter handles auth per-route)
│       └── filter/
│           └── JwtAuthFilter.java      ← Extracts Bearer token, injects X-User-Id / X-User-Role headers
│       └── fallback/
│           └── FallbackController.java ← Circuit breaker fallback responses
│
├── order-service/
│   └── src/main/java/com/shopflow/order/
│       ├── controller/
│       │   └── OrderController.java    ← POST /api/orders, GET /api/orders, GET /api/orders/{id}
│       ├── model/
│       │   └── Order.java              ← Statuses: PENDING → PAYMENT_PROCESSING → COMPLETED | FAILED
│       ├── saga/
│       │   └── OrderSagaOrchestrator.java  ← Starts saga, listens for payment.response
│       ├── config/
│       │   └── RabbitMqConfig.java     ← Declares saga.exchange + all queues/bindings
│       └── repository/
│           └── OrderRepository.java
│
├── payment-service/
│   └── src/main/java/com/shopflow/payment/
│       ├── controller/
│       │   └── PaymentController.java  ← GET/POST /api/payments, retry endpoint
│       ├── model/
│       │   └── Payment.java            ← Statuses: SUCCESS | FAILED
│       ├── service/
│       │   └── PaymentService.java     ← Business logic, idempotency guard, fail threshold
│       ├── saga/
│       │   └── PaymentSagaParticipant.java ← Listens on payment.request, replies on payment.response
│       └── config/
│           └── SecurityConfig.java     ← Permits /actuator/health, secures all other routes
│
├── frontend-service/
│   └── index.html                  ← Static frontend served via Nginx
│
├── prometheus/prometheus.yml
├── loki/loki-config.yml
├── tempo/tempo-config.yml
└── grafana/provisioning/
```

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)
- Java 17+ and Maven (only needed for Option B)

### Option A — Full Docker (recommended)

Builds all JARs and starts every service in one command.

```bash
git clone https://github.com/your-username/shopflow.git
cd shopflow

# Add your JWT secret to the gateway before starting
# In api-gateway/src/main/resources/application.yml, add:
#   app:
#     jwt:
#       secret: your-super-secret-key-at-least-32-chars

docker compose up -d
```

> ⚠️ First build takes 3–5 minutes — Maven downloads all dependencies inside the container. Subsequent builds use Docker layer cache and are much faster.

Check everything is healthy:

```bash
docker compose ps

# All four app services should show "healthy" or "running"
curl http://localhost:8081/actuator/health   # order-service
curl http://localhost:8082/actuator/health   # payment-service
curl http://localhost:8086/actuator/health   # api-gateway
```

Tear down (keeps data volumes):
```bash
docker compose down
```

Tear down and wipe all data:
```bash
docker compose down -v
```

---

## Configuration

### Environment Variables (Docker Compose)

| Variable | Default | Service | Notes |
|---|---|---|---|
| `DB_HOST` | `postgres-order` / `postgres-payment` | order, payment | Postgres hostname |
| `DB_NAME` | `orderdb` / `paymentdb` | order, payment | Database name |
| `DB_USER` | `order_user` / `payment_user` | order, payment | DB user |
| `DB_PASS` | `order_pass` / `payment_pass` | order, payment | DB password |
| `RABBITMQ_HOST` | `rabbitmq` | order, payment | RabbitMQ hostname |
| `RABBITMQ_USER` | `guest` | order, payment | RabbitMQ user |
| `RABBITMQ_PASS` | `guest` | order, payment | RabbitMQ password |
| `PAYMENT_FAIL_THRESHOLD` | `500.00` | payment | Orders above this amount will simulate payment failure |
| `PAYMENT_MAX_AMOUNT` | `10000.00` | payment | Maximum allowed order amount |
| `OTEL_ENDPOINT` | `http://tempo:4317` | all | OpenTelemetry collector endpoint |
| `LOKI_URL` | `http://loki:3100` | all | Loki log aggregation endpoint |

### JWT Secret

The gateway's `JwtUtil` requires `app.jwt.secret` — a string of at least 32 characters (HMAC-SHA256 requirement). Set it in `application.yml` or pass it as `APP_JWT_SECRET` environment variable. **The same secret must be used by any service that issues tokens.**

---

## API Reference

All requests go through the gateway on port `8086`. Services are also directly accessible on their own ports during development.

### Orders — `POST /api/orders`

Create a new order and start the Saga.

```bash
curl -X POST http://localhost:8086/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "product": "Laptop Pro",
    "customerId": "customer-123",
    "amount": 299.99
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "product": "Laptop Pro",
  "customerId": "customer-123",
  "amount": 299.99,
  "status": "PAYMENT_PROCESSING",
  "createdAt": "2026-06-09T10:00:00"
}
```

The order status will asynchronously transition to `COMPLETED` or `FAILED` once the payment response arrives via RabbitMQ.

---

### Orders — `GET /api/orders`

List all orders.

```bash
curl http://localhost:8086/api/orders
```

---

### Orders — `GET /api/orders/{id}`

Get a single order by UUID.

```bash
curl http://localhost:8086/api/orders/550e8400-e29b-41d4-a716-446655440000
```

---

### Orders — Order Statuses

| Status | Meaning |
|---|---|
| `PENDING` | Order created, not yet sent to payment |
| `PAYMENT_PROCESSING` | PaymentRequestEvent published to RabbitMQ |
| `COMPLETED` | Payment succeeded |
| `FAILED` | Payment failed — order compensated |
| `COMPENSATED` | Explicitly rolled back |

---

### Payments — `GET /api/payments`

List all payments (debug/admin).

```bash
curl http://localhost:8086/api/payments
```

---

### Payments — `GET /api/payments/{id}`

Get a payment by UUID.

```bash
curl http://localhost:8086/api/payments/660e8400-e29b-41d4-a716-446655440000
```

---

### Payments — `GET /api/payments/order/{orderId}`

Get all payments for a given order.

```bash
curl http://localhost:8086/api/payments/order/550e8400-e29b-41d4-a716-446655440000
```

---

### Payments — `POST /api/payments`

Manually trigger a payment (useful for testing outside the Saga flow).

```bash
curl -X POST http://localhost:8086/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "customer-123",
    "amount": 299.99,
    "product": "Laptop Pro"
  }'
```

---

### Payments — `POST /api/payments/order/{orderId}/retry`

Retry a failed payment.

```bash
curl -X POST http://localhost:8086/api/payments/order/550e8400-e29b-41d4-a716-446655440000/retry \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "amount": 299.99
  }'
```

---

### Circuit Breaker Fallbacks

When a service is down, the gateway returns a structured fallback instead of hanging:

```bash
docker compose stop service-order

curl http://localhost:8086/api/orders
# Returns:
{
  "service": "order-service",
  "status": "UNAVAILABLE",
  "message": "Order service is temporarily unavailable. Please try again shortly.",
  "timestamp": "2026-06-09T10:00:00Z"
}
```

---

## The Saga Pattern

ShopFlow implements the **Saga choreography pattern** for distributed transactions across order and payment services. No central coordinator — services react to events.

### Happy Path (amount ≤ $500)

```
Client
  │
  ▼
POST /api/orders  ──▶  Order Service
                           │  1. Persist order (PENDING)
                           │  2. Set status → PAYMENT_PROCESSING
                           │  3. Publish PaymentRequestEvent
                           │
                           ▼
                       RabbitMQ: payment.request
                           │
                           ▼
                       Payment Service
                           │  4. Validate amount
                           │  5. Check idempotency (no duplicate)
                           │  6. Process payment → SUCCESS
                           │  7. Publish PaymentResponseEvent(success=true)
                           │
                           ▼
                       RabbitMQ: payment.response
                           │
                           ▼
                       Order Service
                           │  8. Set order status → COMPLETED ✅
```

### Compensation Path (amount > $500)

```
  ...steps 1–3 same as above...

                       Payment Service
                           │  4. Amount exceeds fail threshold ($500)
                           │  5. Persist payment record (FAILED)
                           │  6. Publish PaymentResponseEvent(success=false)
                           │
                           ▼
                       RabbitMQ: payment.response
                           │
                           ▼
                       Order Service
                           │  7. Set order status → FAILED ❌ (compensated)
```

### Testing the Saga

```bash
# ✅ Should COMPLETE (amount under $500 threshold)
curl -X POST http://localhost:8086/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product": "Book", "customerId": "c1", "amount": 49.99}'

# ❌ Should FAIL and trigger compensation (amount over $500 threshold)
curl -X POST http://localhost:8086/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product": "Server", "customerId": "c1", "amount": 999.99}'

# ❌ Should FAIL — exceeds max amount ($10,000)
curl -X POST http://localhost:8086/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product": "Data Center", "customerId": "c1", "amount": 15000.00}'
```

Watch the Saga flow in real time by opening **RabbitMQ Management UI** at `http://localhost:15672` (guest / guest) and watching the `payment.request` and `payment.response` queues while firing requests.

---

## Observability

The full observability stack is included and pre-configured. All services emit metrics, logs, and traces automatically.

### Grafana — `http://localhost:3001`

**Credentials:** `admin` / `shopflow`

Pre-provisioned dashboards for all services including request rates, error rates, JVM metrics, and Saga flow visibility.

### Prometheus — `http://localhost:9090`

Scrapes `/actuator/prometheus` from all services every 15 seconds. Useful queries:

```promql
# Request rate per service
rate(http_server_requests_seconds_count[1m])

# Active RabbitMQ listeners
rabbitmq_queue_messages_ready
```

### Distributed Tracing — Grafana Tempo

Every request gets a `traceId` injected into logs. In Grafana, go to **Explore → Tempo** and paste a `traceId` from a log line to see the full distributed trace across services.

### Log Aggregation — Grafana Loki

All service logs are shipped to Loki. In Grafana, go to **Explore → Loki** and query:

```logql
{application="service-order"} |= "SAGA"
{application="service-payment"} |= "orderId"
```

### RabbitMQ Management — `http://localhost:15672`

**Credentials:** `guest` / `guest`

Monitor queue depths, message rates, and consumer status in real time.

---

## Testing

### Health checks

```bash
# All at once
curl -s http://localhost:8081/actuator/health | python3 -m json.tool
curl -s http://localhost:8082/actuator/health | python3 -m json.tool
curl -s http://localhost:8086/actuator/health | python3 -m json.tool
```

### Service status endpoints

```bash
curl http://localhost:8081/api/orders/status
curl http://localhost:8082/api/payments/status
```

### Full smoke test script

```bash
#!/bin/bash
set -e

BASE="http://localhost:8086"

echo "--- Health checks ---"
curl -sf $BASE/actuator/health > /dev/null && echo "Gateway ✅"
curl -sf http://localhost:8081/actuator/health > /dev/null && echo "Order service ✅"
curl -sf http://localhost:8082/actuator/health > /dev/null && echo "Payment service ✅"

echo ""
echo "--- Placing a successful order (amount=100) ---"
RESULT=$(curl -sf -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"Widget","customerId":"smoke-test","amount":100.00}')
echo $RESULT | python3 -m json.tool
ORDER_ID=$(echo $RESULT | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo ""
echo "--- Waiting 2s for Saga to complete ---"
sleep 2

echo "--- Checking order status ---"
curl -sf $BASE/api/orders/$ORDER_ID | python3 -m json.tool

echo ""
echo "--- Placing a failing order (amount=999) ---"
curl -sf -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"Expensive Thing","customerId":"smoke-test","amount":999.00}' \
  | python3 -m json.tool

echo ""
echo "All tests passed ✅"
```

---

## Ports Reference

| URL | Service |
|---|---|
| `http://localhost:8086` | API Gateway — use this for all client requests |
| `http://localhost:8081` | Order Service (direct, bypass gateway) |
| `http://localhost:8082` | Payment Service (direct, bypass gateway) |
| `http://localhost:3000` | Frontend UI |
| `http://localhost:15672` | RabbitMQ Management (guest / guest) |
| `http://localhost:9090` | Prometheus |
| `http://localhost:3001` | Grafana (admin / shopflow) |
| `http://localhost:3100` | Loki |
| `http://localhost:3200` | Tempo |
| `localhost:5433` | PostgreSQL — orderdb (order_user / order_pass) |
| `localhost:5434` | PostgreSQL — paymentdb (payment_user / payment_pass) |
| `localhost:5672` | RabbitMQ AMQP |

---

## Roadmap

- [ ] JWT issuance endpoint — `POST /api/auth/login`
- [ ] Attach `JwtAuthFilter` to protected routes in gateway `application.yml`
- [ ] Dead Letter Queue (DLQ) for failed Saga messages
- [ ] Order history endpoint with pagination
- [ ] Inventory service with stock reservation step in Saga
- [ ] Kubernetes manifests (Helm chart)
- [ ] GitHub Actions CI pipeline

---

## License

MIT