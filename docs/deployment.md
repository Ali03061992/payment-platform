# Déploiement & CI/CD

## Docker Compose (`deploy/docker-compose.yml`)

```bash
docker compose up --build
```

Services : `mysql` (4 bases, init par scripts), `rabbitmq` (management), 5 services Spring Boot (JRE 26 slim), `angular` (nginx, sert le build + proxy `/api` → gateway). Health checks sur chaque conteneur ; `depends_on: condition: service_healthy` ; reseau `payment-net`.

| Service | Image de base | Ports |
|---|---|---|
| mysql | `mysql:8.4` | 3307:3306, health `mysqladmin ping` |
| rabbitmq | `rabbitmq:4-management` | 5673:5672, 15673:15672 |
| identity-service | `eclipse-temurin:26-jre` | — |
| organization-service | `eclipse-temurin:26-jre` | — |
| payment-service | `eclipse-temurin:26-jre` | — |
| notification-service | `eclipse-temurin:26-jre` | — |
| api-gateway | `eclipse-temurin:26-jre` | 8081:8081 |
| angular | `nginx:1.27-alpine` | 8080:80 |

### Builds multi-étapes (Maven)

```dockerfile
FROM maven:3.9-eclipse-temurin-26 AS build
COPY backend /app
RUN mvn -pl <service> -am package -DskipTests
FROM eclipse-temurin:26-jre
COPY --from=build /app/<service>/target/*.jar app.jar
```

## Configuration

Tout par variables d'environnement (`SPRING_DATASOURCE_URL`, `SPRING_RABBITMQ_HOST`, `JWT_SECRET`, …). Un `.env.example` documente l'ensemble ; **aucun secret dans le repository**.

## CI/CD (GitHub Actions, `.github/workflows/ci.yml`)

```yaml
jobs:
  backend:
    - checkout, setup-java 26 (temurin)
    - mvnw -B verify              # compile + unit + integration (services: mysql+rabbitmq Testcontainers)
    - mvnw -B package -DskipTests # artifacts
  frontend:
    - setup-node 18
    - npm ci && npm run lint && npm test && npm run build
  e2e:
    - docker compose -f deploy/docker-compose.yml up --build -d
    - npx playwright test
  docker-build:
    - docker buildx build chaque image (dépend de backend+frontend)
```

Le job e2e dépend de `backend` et `frontend` ; le pipeline **échoue si un test échoue** (fail-fast).

## Stratégie de branche

- `main` : protégé, exige CI verte.
- `release` : tag `vX.Y.Z` → déploiement staging via compose ; production = mêmes images taguées.

## Runbook local (sans Docker Desktop)

Tests unitaires uniquement : `./mvnw -DskipITs test` ; tests d'intégration nécessitent Docker (Testcontainers).