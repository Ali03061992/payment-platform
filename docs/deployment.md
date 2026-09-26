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

Tout par variables d'environnement (`SPRING_DATASOURCE_URL`, `SPRING_RABBITMQ_HOST`, `JWT_SECRET`, …). Un `.env.example` documente l'ensemble (`deploy/.env.example`) ; **aucun secret dans le repository**.

| Variable | Défaut / sens | Preuve |
|---|---|---|
| `RATE_LIMIT_PER_MINUTE` | 100 (bucket général gateway) | `deploy/.env.example:32`, `docker-compose.yml:213` |
| `RATE_LIMIT_AUTH_PER_MINUTE` | **10** en prod/compose, **1000** en local/E2E/CI | `application.yml:21`, `application-local.yml:8`, `ci.yml:399`, `.run/5_API_Gateway` |
| `INTERNAL_SECRET` | obligatoire au boot (fail-fast, sans défaut en prod) | `InternalSecretValidator.java`, `application-prod.yml` |
| `FIREBASE_API_KEY`, `FIREBASE_AUTH_DOMAIN`, `FIREBASE_PROJECT_ID`, `FIREBASE_STORAGE_BUCKET`, `FIREBASE_MESSAGING_SENDER_ID`, `FIREBASE_APP_ID`, `FCM_VAPID_KEY` | vides = push désactivé proprement (M7) | `deploy/.env.example:41-47` |

## Push Firebase/VAPID au déploiement (M7, front)

Clés **runtime uniquement**, jamais committées (`src/assets/env.js` local = vide) :

```bash
envsubst < payment-platform-ui/src/assets/env.template.js \
  > <dist>/browser/assets/env.js
```

Le SW (`firebase-messaging-sw.js`) et `environment.ts` lisent `window.__env`
(`FIREBASE_*`, `FCM_VAPID_KEY`) ; sans clés, le push est désactivé proprement.

## PWA dataGroups (M6)

`ngsw-config.json` : groupe prioritaire **`api-financial`** (`/api/payments/**`,
`/api/orders/**`, `freshness`, `maxAge: 5m`, `timeout: 5s`) devant `api-cache`
(`/api/auth/me`, 1h) et `api-network` (`/api/**`, 24h) — aucun cache financier > 5 min.

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
    - npx cypress run   # avec RATE_LIMIT_AUTH_PER_MINUTE=1000 + INTERNAL_SECRET=test-internal-secret (ci.yml:399-400)
  docker-build:
    - docker buildx build chaque image (dépend de backend+frontend)
```

Le job e2e dépend de `backend` et `frontend` ; le pipeline **échoue si un test échoue** (fail-fast).

## Stratégie de branche

- `main` : protégé, exige CI verte.
- `release` : tag `vX.Y.Z` → déploiement staging via compose ; production = mêmes images taguées.

## Runbook local (sans Docker Desktop)

Tests unitaires uniquement : `./mvnw -DskipITs test` ; tests d'intégration nécessitent Docker (Testcontainers).