# Payment Platform - Local Development Guide

## Architecture Overview

```
                          ┌─────────────────┐
                          │   Angular (SPA)  │
                          │    Port 8080     │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │   Nginx Proxy   │
                          │  /api/ → GW     │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │   API Gateway   │
                          │    Port 8081     │
                          └────────┬────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
    ┌─────────▼─────────┐ ┌───────▼───────┐ ┌─────────▼─────────┐
    │  Identity Service │ │  Org Service  │ │  Payment Service  │
    │     Port 8082     │ │   Port 8083   │ │     Port 8084     │
    └─────────┬─────────┘ └───────┬───────┘ └─────────┬─────────┘
              │                    │                    │
              └────────────────────┼────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
    ┌─────────▼─────────┐ ┌───────▼───────┐ ┌─────────▼─────────┐
    │  Notification Svc │ │   MySQL 8.4   │ │  RabbitMQ 4       │
    │     Port 8085     │ │   Port 3307   │ │  Mgmt: 15673      │
    └───────────────────┘ │   AMQP: 5673  │ └───────────────────┘
                          └───────────────┘
```

---

## Services & URLs

### Frontend

| Service | URL | Description |
|---------|-----|-------------|
| **Angular App** | [http://localhost:8080](http://localhost:8080) | SPA frontend (Angular 16) |

### Backend

| Service | Internal Port | External Port | URL | Description |
|---------|:------------:|:------------:|-----|-------------|
| **API Gateway** | 8081 | 8081 | [http://localhost:8081](http://localhost:8081) | Entry point, routing, JWT, CORS |
| **Identity Service** | 8082 | — | Internal only | Users, roles, JWT, RBAC |
| **Organization Service** | 8083 | — | Internal only | Suppliers, shops, org management |
| **Payment Service** | 8084 | — | Internal only | Payments, outbox pattern |
| **Notification Service** | 8085 | — | Internal only | Notifications, email, SMS |

### Infrastructure

| Service | Port | URL | Credentials |
|---------|:----:|-----|-------------|
| **MySQL 8.4** | 3307 | `localhost:3307` | `payment_app` / `app-password-change-me` |
| **MySQL Root** | 3307 | `localhost:3307` | `root` / `root-password-change-me` |
| **RabbitMQ Management** | 15673 | [http://localhost:15673](http://localhost:15673) | `payment` / `rabbit-password-change-me` |
| **RabbitMQ AMQP** | 5673 | `localhost:5673` | `payment` / `rabbit-password-change-me` |

---

## API Gateway Routes

All API calls go through the gateway at `http://localhost:8081/api/...`:

| Route Pattern | Target Service |
|---------------|----------------|
| `/api/auth/**` | Identity Service (8082) |
| `/api/users/**` | Identity Service (8082) |
| `/api/suppliers/{supplierId}/agents/**` | Identity Service (8082) |
| `/api/admin/suppliers/**` | Organization Service (8083) |
| `/api/admin/shops/**` | Organization Service (8083) |
| `/api/suppliers/**` | Organization Service (8083) |
| `/api/shops/**` | Organization Service (8083) |
| `/api/organizations/**` | Organization Service (8083) |
| `/api/payments/**` | Payment Service (8084) |
| `/api/notifications/**` | Notification Service (8085) |

---

## MySQL Databases

| Database | Service | Description |
|----------|---------|-------------|
| `identity_db` | Identity Service | Users, roles, user_roles, refresh_tokens |
| `organization_db` | Organization Service | Suppliers, shops, organizations |
| `payment_db` | Payment Service | Payments, outbox events, processed events |
| `notification_db` | Notification Service | Notifications, templates |

All databases are initialized via `deploy/mysql-init/01-init-databases.sql` on first startup.

---

## RabbitMQ

- **Management UI**: [http://localhost:15673](http://localhost:15673)
- **AMQP Port**: 5673 (mapped from container 5672)
- **Default User**: `payment`
- **Default Password**: `rabbit-password-change-me`

Used by identity, organization, payment, and notification services for asynchronous event-driven communication (Spring AMQP / RabbitMQ).

---

## SpringDoc / Swagger UI

Each backend service exposes Swagger UI at:

| Service | Swagger URL |
|---------|-------------|
| Identity Service | `http://localhost:8081/api/auth/swagger-ui.html` (via gateway) |
| Organization Service | `http://localhost:8081/api/organizations/swagger-ui.html` (via gateway) |
| Payment Service | `http://localhost:8081/api/payments/swagger-ui.html` (via gateway) |
| Notification Service | `http://localhost:8081/api/notifications/swagger-ui.html` (via gateway) |

> **Note**: Swagger UI is only accessible through the API Gateway proxy.

---

## Actuator Endpoints

All backend services expose health and info endpoints:

| Service | Health Check |
|---------|-------------|
| Identity Service | `http://localhost:8081/api/auth/actuator/health` |
| Organization Service | `http://localhost:8081/api/organizations/actuator/health` |
| Payment Service | `http://localhost:8081/api/payments/actuator/health` |
| Notification Service | `http://localhost:8081/api/notifications/actuator/health` |
| API Gateway | `http://localhost:8081/actuator/health` |

---

## Quick Start

```bash
cd deploy/

# Start everything (first time or after code changes)
docker compose up --build -d

# Check status
docker compose ps

# View logs (all services)
docker compose logs -f

# View logs (single service)
docker compose logs -f identity-service
docker compose logs -f payment-service
```

---

## Stop & Restart

```bash
# Stop all services (keeps data volumes)
docker compose down

# Stop and remove volumes (full reset, lose all data)
docker compose down -v

# Stop and remove everything including built images
docker compose down -v --rmi local

# Restart a single service
docker compose restart identity-service

# Rebuild and restart a single service
docker compose up --build -d identity-service
```

---

## Port Mapping

| Service | Container Port | Host Port | URL |
|---------|:--------------:|:---------:|-----|
| Angular (Nginx) | 80 | **8080** | http://localhost:8080 |
| API Gateway | 8081 | **8081** | http://localhost:8081 |
| Identity Service | 8082 | — | Internal only (via gateway) |
| Organization Service | 8083 | — | Internal only (via gateway) |
| Payment Service | 8084 | — | Internal only (via gateway) |
| Notification Service | 8085 | — | Internal only (via gateway) |
| MySQL | 3306 | **3307** | localhost:3307 |
| RabbitMQ AMQP | 5672 | **5673** | localhost:5673 |
| RabbitMQ Management | 15672 | **15673** | http://localhost:15673 |

> **Note**: Backend services (identity, org, payment, notification) are **not exposed** on host ports. They are only accessible through the API Gateway proxy at `http://localhost:8081/api/...`.

---

## Environment Variables

Defined in `deploy/.env`:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `JWT_SECRET` | `dev-only-secret-change-me-...` | JWT signing secret |
| `JWT_EXPIRATION` | `30m` | JWT token expiration |
| `MYSQL_ROOT_PASSWORD` | `root-password-change-me` | MySQL root password |
| `MYSQL_USER` | `payment_app` | MySQL application user |
| `MYSQL_PASSWORD` | `app-password-change-me` | MySQL application password |
| `RABBITMQ_USER` | `payment` | RabbitMQ user |
| `RABBITMQ_PASSWORD` | `rabbit-password-change-me` | RabbitMQ password |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile |

---

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 26 |
| Spring Boot | 4.1.0 |
| Spring Framework | 7.0.8 |
| Spring Cloud | 2025.1.2 |
| Angular | 16 |
| Node.js | 18 (build only) |
| MySQL | 8.4 |
| RabbitMQ | 4 |
| Flyway | 12.4.0 |
| Nginx | 1.27-alpine |
| Docker | Multi-stage builds |

---

## Project Structure

```
payment-platform/
├── backend/
│   ├── pom.xml                          # Parent POM
│   ├── shared-lib/                      # Shared kernel (JWT, audit, outbox, etc.)
│   ├── api-gateway/                     # Spring Cloud Gateway
│   ├── identity-service/                # Users, roles, JWT auth
│   ├── organization-service/            # Suppliers, shops
│   ├── payment-service/                 # Payment processing, outbox
│   ├── notification-service/            # Notifications
│   └── Dockerfile                       # Multi-stage Maven build
├── payment-platform-ui/                 # Angular 16 source
├── frontend/
│   └── Dockerfile                       # Node build + Nginx
├── deploy/
│   ├── docker-compose.yml               # Full stack orchestration
│   ├── .env                             # Environment variables
│   ├── nginx/default.conf               # Nginx config (SPA + API proxy)
│   └── mysql-init/                      # Database initialization scripts
├── backend/.dockerignore
└── .dockerignore
```

---

## Troubleshooting

### Services fail to start
```bash
docker compose logs <service-name>
```

### MySQL not ready
Services wait for MySQL healthcheck before starting. If MySQL takes long:
```bash
docker compose up mysql -d
# Wait for healthy status
docker compose ps
# Then start the rest
docker compose up -d
```

### Reset everything
```bash
docker compose down -v --rmi local
docker compose up --build -d
```

### Connect to MySQL directly
```bash
docker compose exec mysql mysql -u payment_app -papp-password-change-me identity_db
```

### Connect to RabbitMQ
```bash
# Management UI: http://localhost:15673
# CLI:
docker compose exec rabbitmq rabbitmqctl list_queues
```
