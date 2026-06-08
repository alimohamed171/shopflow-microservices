# 🛒 ShopFlow — Spring Boot E-Commerce

3-service microservices backend built with Java 17 + Spring Boot 3.

| Service | Port | Database |
|---|---|---|
| `api-gateway` | **9092** | — |
| `core-service` | **9090** | MySQL 8 |
| `payment-service` | **9091** | PostgreSQL 15 |

Infrastructure: **RabbitMQ** on 5672 (UI on 15672).

---

## Option A — Local Dev (recommended while learning)

Run infrastructure in Docker, services with Maven on your machine.
This means fast restarts when you change code — no image rebuild needed.

### 1. Start infrastructure

```bash
docker compose -f docker-compose.infra.yml up -d
```

Wait ~20 seconds, then verify:
```bash
# MySQL
docker exec shopflow-mysql mysqladmin ping -ushopflow -pshopflow_pass

# PostgreSQL
docker exec shopflow-postgres pg_isready -U shopflow

# RabbitMQ UI
open http://localhost:15672   # guest / guest
```

### 2. Run Core Service

```bash
cd core-service
mvn spring-boot:run
```

Test it:
```bash
curl http://localhost:9090/api/core/health/time
```

Expected response:
```json
{
  "service": "core-service",
  "status": "UP",
  "serverTime": "2025-01-01T12:00:00+00:00",
  "port": 9090
}
```

### 3. Run Payment Service

```bash
cd payment-service
mvn spring-boot:run
```

### 4. Run API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

Test routing through the Gateway:
```bash
curl http://localhost:9092/api/core/health/time
```

Same response as step 2 — but now routed through the Gateway on port 9092. ✅

---

## Option B — Full Docker (all services in containers)

```bash
cp .env .env          # already done — check values if needed
docker compose up -d  # builds all JARs + starts everything
```

> ⚠️ First build takes ~3-5 minutes (Maven downloads dependencies).
> Subsequent builds are much faster (Docker layer cache).

Check everything is up:
```bash
docker compose ps
curl http://localhost:9092/api/core/health/time
```

Tear it all down (keeps data volumes):
```bash
docker compose down
```

Tear it all down AND delete all data:
```bash
docker compose down -v
```

---

## Running Tests

```bash
# All tests (no Docker needed — uses H2 in-memory)
cd core-service
mvn test

# Single test class
mvn test -Dtest=HealthControllerTest
```

---

## Project Structure

```
shopflow/
├── docker-compose.yml           ← Full stack (all services + infra)
├── docker-compose.infra.yml     ← Infra only (MySQL, PostgreSQL, RabbitMQ)
├── .env                         ← Environment variables (don't commit!)
├── pom.xml                      ← Parent POM (manages all modules)
│
├── api-gateway/                 ← Port 9092 | Spring Cloud Gateway
│   └── src/main/java/com/shopflow/gateway/
│       ├── ApiGatewayApplication.java
│       ├── config/
│       │   ├── JwtUtil.java         ← Validates JWT signature
│       │   └── SecurityConfig.java  ← WebFlux security (permit all → filter handles auth)
│       └── filter/
│           └── JwtAuthFilter.java   ← Extracts + validates Bearer token per route
│
├── core-service/                ← Port 9090 | MySQL | Main business logic
│   └── src/main/java/com/shopflow/core/
│       ├── CoreServiceApplication.java
│       ├── config/
│       │   ├── RabbitMQConfig.java  ← Exchanges, queues, bindings
│       │   └── SecurityConfig.java  ← JWT filter + permit rules
│       ├── controller/
│       │   └── HealthController.java  ← GET /api/core/health/time ✅
│       ├── service/             ← Business logic lives here
│       ├── repository/          ← Spring Data JPA repos
│       ├── entity/              ← JPA entities (@Entity classes)
│       ├── dto/
│       │   ├── request/         ← Incoming JSON bodies
│       │   └── response/        ← Outgoing JSON responses
│       ├── messaging/
│       │   ├── producer/        ← RabbitTemplate.convertAndSend(...)
│       │   └── consumer/        ← @RabbitListener methods
│       ├── security/            ← JwtFilter, UserDetailsService (TODO)
│       ├── exception/           ← @ControllerAdvice, custom exceptions (TODO)
│       └── util/                ← Shared helpers
│
└── payment-service/             ← Port 9091 | PostgreSQL | Payments
    └── src/main/java/com/shopflow/payment/
        ├── PaymentServiceApplication.java
        ├── config/
        │   ├── RabbitMQConfig.java
        │   └── SecurityConfig.java
        ├── controller/          ← (TODO: PaymentController, WalletController)
        ├── service/             ← (TODO: PaymentService, WalletService)
        ├── repository/          ← (TODO: PaymentRepository, WalletRepository)
        ├── entity/              ← (TODO: Payment, Wallet, Transaction)
        ├── dto/
        ├── messaging/
        └── security/
```

---

## Ports Reference

| URL | What |
|---|---|
| `http://localhost:9092` | API Gateway (use this for all requests) |
| `http://localhost:9090` | Core Service (direct, bypass Gateway) |
| `http://localhost:9091` | Payment Service (direct, bypass Gateway) |
| `http://localhost:15672` | RabbitMQ Management UI (guest/guest) |
| `localhost:3306` | MySQL (user: shopflow / pass: shopflow_pass) |
| `localhost:5432` | PostgreSQL (user: shopflow / pass: shopflow_pass) |

---

## Environment Variables

All values live in `.env`. Key ones:

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | `shopflow-super-secret...` | **Change in production. Must be same across all 3 services.** |
| `MYSQL_PASSWORD` | `shopflow_pass` | Core Service DB password |
| `POSTGRES_PASSWORD` | `shopflow_pass` | Payment Service DB password |
| `RABBITMQ_USER` | `guest` | RabbitMQ user |

---

## What's Working Now

- [x] Project structure with correct Maven multi-module layout
- [x] All 3 `pom.xml` files with proper dependencies
- [x] All 3 `application.yml` files with correct ports (9090 / 9091 / 9092)
- [x] Gateway JWT filter (validates tokens, injects `X-User-Id` / `X-User-Role` headers)
- [x] Gateway routing (public routes bypass auth, protected routes go through `JwtAuthFilter`)
- [x] RabbitMQ config in Core + Payment (exchanges, queues, bindings declared)
- [x] Security config in all 3 services
- [x] `GET /api/core/health/time` — test endpoint
- [x] Flyway migration stubs (ready for your first real `CREATE TABLE`)
- [x] Docker Compose (full stack + infra-only variant)
- [x] Unit test for `HealthController`
- [x] Test profile using H2 (no Docker needed for tests)

## What to Build Next

1. `JwtFilter` in core-service (`security/` package) — reads `X-User-Id` header from Gateway
2. `User` entity + `UserRepository` + first real Flyway migration
3. `POST /api/auth/register` and `POST /api/auth/login`
