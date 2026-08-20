# Payment Platform

Plateforme professionnelle de gestion des paiements unitaires entre **fournisseurs** et **boutiques**.

Architecture **microservices** appliquant **DDD**, **Clean/Hexagonal Architecture**, **SOLID** et **Clean Code**. Chaque microservice possède **sa propre base MySQL** ; les échanges inter-services sont événementiels (RabbitMQ) avec **Outbox Pattern** pour les événements critiques.

## Stack (versions stables, août 2026)

| Brique | Version |
|---|---|
| Java | 26 LTS |
| Spring Boot | 4.1.0 (Spring Framework 7, Hibernate 7.2) |
| Spring Cloud | 2025.1.2 "Oakwood" (compatible Boot 4.1) |
| MySQL | 8.4 LTS |
| RabbitMQ | 4.x |
| Angular | 22.1 (Signals, zoneless) |
| Outillage | Maven Wrapper, Flyway, OpenAPI, JUnit 5, Mockito, AssertJ, Testcontainers, Playwright |

## Structure

```
payment-platform/
├── backend/
│   ├── pom.xml                     # parent Maven
│   ├── shared-lib/                 # kernel partagé : events, security, outbox, DDD base
│   ├── identity-service/           # utilisateurs, rôles, JWT, RBAC
│   ├── organization-service/       # fournisseurs, boutiques, relations
│   ├── payment-service/            # paiements, machine à états, audit, outbox
│   ├── notification-service/       # notifications persistées + SSE temps réel
│   └── api-gateway/                # point d'entrée unique : routing, JWT, CORS, rate-limit
├── frontend/                       # Angular 22
├── deploy/
│   ├── docker-compose.yml          # tout l'infra : `docker compose up --build`
│   └── nginx/                      # servira l'app Angular
├── docs/                           # documentation complète (voir ci-dessous)
└── .github/workflows/ci.yml        # pipeline CI/CD
```

## Rôles

`SYSTEM_ADMIN`, `SUPPLIER_ADMIN`, `SUPPLIER_AGENT`, `SHOP_ADMIN`, `SHOP_AGENT`.

## Démarrage

```bash
docker compose up --build
```

Puis ouvrir http://localhost:8080 (Angular) / http://localhost:8081 (Gateway Swagger).

## Documentation

| Sujet | Fichier |
|---|---|
| Architecture globale & décisions | [docs/architecture.md](docs/architecture.md) |
| DDD (aggregates, entities, VOs) | [docs/ddd.md](docs/ddd.md) |
| Bounded contexts | [docs/bounded-contexts.md](docs/bounded-contexts.md) |
| Règles métier | [docs/business-rules.md](docs/business-rules.md) |
| Modèle de données MySQL | [docs/database.md](docs/database.md) |
| API REST | [docs/api.md](docs/api.md) |
| Sécurité (JWT, RBAC) | [docs/security.md](docs/security.md) |
| Événements & Outbox | [docs/events.md](docs/events.md) |
| Notifications temps réel | [docs/notifications.md](docs/notifications.md) |
| Stratégie de tests | [docs/testing.md](docs/testing.md) |
| Déploiement & CI/CD | [docs/deployment.md](docs/deployment.md) |

## Comptes de démonstration (seed)

| Compte | Rôle | Organisation |
|---|---|---|
| `system.admin` / `Admin@123` | SYSTEM_ADMIN | — |
| `supplier.admin` / `Supplier@123` | SUPPLIER_ADMIN | Fournisseur ABC |
| `shop.admin` / `Shop@123` | SHOP_ADMIN | Boutique Tunis Centre |
| `shop.agent` / `Agent@123` | SHOP_AGENT | Boutique Tunis Centre |

## CI/CD

`.github/workflows/ci.yml` : compile → unit tests → integration tests (Testcontainers) → security tests → frontend tests → e2e (Playwright) → docker build. Le pipeline **échoue** si un test échoue.

> Note : l'implémentation est conçue pour s'exécuter dans Docker (Testcontainers + Compose). Sans Docker Desktop (WSL2) installé, les tests d'intégration/E2E ne peuvent pas s'exécuter en local.