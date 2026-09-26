# Payment Platform

Plateforme professionnelle de gestion des paiements unitaires entre **fournisseurs** et **boutiques**.

Architecture **microservices** appliquant **DDD**, **Clean/Hexagonal Architecture**, **SOLID** et **Clean Code**. Chaque microservice possède **sa propre base MySQL** ; les échanges inter-services sont événementiels (RabbitMQ) avec **Outbox Pattern** pour les événements critiques.

## Stack

| Brique | Version |
|---|---|
| Java | 26 LTS |
| Spring Boot | 4.1.0 (Spring Framework 7, Hibernate 7.4) |
| Spring Cloud | 2025.1.2 (compatible Boot 4.1) |
| MySQL | 8.4 LTS |
| Elasticsearch | 9.0.0 |
| RabbitMQ | 4.x |
| Angular | 16 |
| Docker | Multi-stage builds |

## Environments (Spring Profiles)

| Profile | Usage | JWT Secret | MySQL Host | RabbitMQ Host | Redis Host |
|---|---|---|---|---|---|
| `local` | IDE + Docker deps | Default dev secret | `localhost:3307` | `localhost:5673` | `localhost:6379` |
| `dev` | Docker Compose full stack | Default dev secret | `mysql` | `rabbitmq` | `redis` |
| `test` | Unit/Integration tests | Test secret | H2 in-memory | Disabled | Disabled |
| `prod` | Production (Render, etc.) | **Required via env var** | Env var required | Env var required | Env var required |

### Activation

- **Par défaut** : `local` (via `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`)
- **IDE** : Lancé automatiquement avec le profil `local`
- **Docker Compose** : `SPRING_PROFILES_ACTIVE=dev` (déjà configuré dans `docker-compose.yml`)
- **Production** : `SPRING_PROFILES_ACTIVE=prod` (déjà configuré dans `render.yaml`)
- **Tests** : Activé automatiquement via `src/test/resources/application.yml`

### Configuration par profile

Chaque microservice possède ses fichiers de configuration :

```
backend/{service}/src/main/resources/
├── application.yml           # Config commune (port, noms)
├── application-local.yml     # Local : localhost avec ports Docker
├── application-dev.yml       # Dev : noms de containers Docker
├── application-prod.yml      # Prod : valeurs depuis env vars uniquement
```

## Structure

```
payment-platform/
├── backend/
│   ├── pom.xml                     # parent Maven
│   ├── shared-lib/                 # kernel partagé : events, security, outbox, DDD base
│   ├── identity-service/           # utilisateurs, rôles, JWT, RBAC
│   ├── organization-service/       # fournisseurs, boutiques, relations
│   ├── payment-service/            # paiements, machine à états, audit, outbox
│   ├── notification-service/       # notifications persistées
│   └── api-gateway/                # point d'entrée unique : routing, JWT, CORS
├── payment-platform-ui/            # Angular 16 frontend
├── deploy/
│   ├── docker-compose.yml          # orchestration complète
│   ├── .env                        # variables d'environnement
│   ├── run.ps1                     # démarrer le projet
│   ├── stop.ps1                    # arrêter le projet
│   ├── nginx/default.conf          # config Nginx (SPA + proxy API)
│   └── mysql-init/                 # scripts d'initialisation MySQL
└── docs/                           # documentation technique
```

## Démarrage rapide

### Prérequis
- Docker Desktop avec WSL2 activé

### Commands

```bash
# Démarrer tout le projet
cd deploy
.\run.ps1

# Ou avec rebuild
.\run.ps1 -Build

# Arrêter (conserve les données)
.\stop.ps1

# Arrêter et supprimer les données
.\stop.ps1 -Clean
```

### Ou manuellement

```bash
cd deploy/
docker compose up --build -d
```

## Développement local (IDE)

Lancer uniquement les dépendances (MySQL, RabbitMQ, Redis, etc.) depuis Docker, puis démarrer les services Spring Boot directement depuis ton IDE pour debugger.

> **Profile actif** : `local` (activé par défaut dans `application.yml`)

### 1. Démarrer les dépendances

```bash
cd deploy
docker compose -f docker-compose.dev.yml up -d
```

### 2. Vérifier que les services sont prêts

```bash
docker compose -f docker-compose.dev.yml ps
```

Tous les containers doivent être `healthy` avant de lancer l'application.

### 3. Lancer les services depuis l'IDE

Dans IntelliJ, configs `.run/` **profil `local`** (`-Dspring.profiles.active=local`) :
`1_Identity_Service` → `5_API_Gateway`, ou `ALL_SERVICES` (tout d'un coup) :

| Service | Main class | Port |
|---|---|---|
| api-gateway | `ApiGatewayApplication` | 8081 |
| identity-service | `IdentityServiceApplication` | 8082 |
| organization-service | `OrganizationServiceApplication` | 8083 |
| payment-service | `PaymentServiceApplication` | 8084 |
| notification-service | `NotificationServiceApplication` | 8085 |

> **Note :** Les services se connectent aux infrastructures Docker sur `localhost` aux ports mappés (3307, 5673, 6379, etc.).

### 3b. Lancer le front en dev

```bash
cd payment-platform-ui
npm run start:local   # ng serve --port 4200 → http://localhost:4200 (apiUrl http://localhost:8081)
```

`http://localhost:8080` = build nginx **docker uniquement** ; en dev IDE, utiliser **4200**
(cf. `cypress.config.ts`, `deploy/run-local.ps1`).

### 3c. Variables d'environnement locales utiles

| Variable | Local / E2E / CI | Prod / compose |
|---|---|---|
| `RATE_LIMIT_AUTH_PER_MINUTE` | `1000` (ne pas flaker les ~70 logins E2E) | `10` (strict B2 + `Retry-After: 60`) |
| `INTERNAL_SECRET` | défauts locaux non committés en prod | **obligatoire au boot** (fail-fast B3) |
| `FIREBASE_*`, `FCM_VAPID_KEY` | vides = push désactivé proprement (M7) | injectées via `envsubst` → `assets/env.js` |

### 4. Arrêter les dépendances

```bash
cd deploy
docker compose -f docker-compose.dev.yml down
```

### Ports des dépendances

| Service | Port | Credentials |
|---|---|---|
| MySQL | `localhost:3307` | `payment_app` / `payment_app` |
| RabbitMQ AMQP | `localhost:5673` | `payment` / `payment` |
| RabbitMQ Management | `localhost:15673` | `payment` / `payment` |
| Redis | `localhost:6379` | — |
| Adminer | `localhost:8086` | — |
| Mailpit | `localhost:8025` | — |

## URLs & Ports

| Service | URL | Description |
|---|---|---|
| **Angular App (docker)** | http://localhost:8080 | Frontend SPA (build nginx) |
| **Angular App (dev IDE)** | http://localhost:4200 | `ng serve` local |
| **Inscription** | http://localhost:8080/register | Créer un compte |
| **Connexion** | http://localhost:8080/login | Se connecter |
| **API Gateway** | http://localhost:8081 | Entry point API |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | Documentation API |
| **Elasticsearch** | http://localhost:9200 | Moteur de recherche |
| **Adminer** | http://localhost:8086 | Interface web MySQL |
| **RabbitMQ** | http://localhost:15673 | Management UI RabbitMQ |

### Ports backend (internes, via gateway)

| Service | Port interne | Accès |
|---|---|---|
| Identity Service | 8082 | Via gateway `/api/auth/**` |
| Organization Service | 8083 | Via gateway `/api/organizations/**` |
| Payment Service | 8084 | Via gateway `/api/payments/**` |
| Notification Service | 8085 | Via gateway `/api/notifications/**` |

### Infrastructure

| Service | Port externe | Credentials |
|---|---|---|
| MySQL | 3307 | `payment_app` / `app-password-change-me` |
| RabbitMQ AMQP | 5673 | `payment` / `rabbit-password-change-me` |
| RabbitMQ Management | 15673 | `payment` / `rabbit-password-change-me` |
| Elasticsearch | 9200 | Pas d'authentification (xpack.security.enabled=false) |

## Elasticsearch

Elasticsearch est utilisé pour la recherche full-text des paiements. Le service tourne sur le port **9200**.

### Accès local

```bash
# Vérifier que le service est actif
curl http://localhost:9200/

# Vérifier la santé du cluster
curl http://localhost:9200/_cluster/health?pretty

# Lister les indices
curl http://localhost:9200/_cat/indices?v

# Voir le mappings de l'indice payments
curl http://localhost:9200/payments/_mapping?pretty

# Rechercher des paiements
curl -X GET "http://localhost:9200/payments/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match_all": {}
  }
}'
```

### Commandes PowerShell

```powershell
# Vérifier que le service est actif
Invoke-RestMethod -Uri "http://localhost:9200/"

# Vérifier la santé du cluster
Invoke-RestMethod -Uri "http://localhost:9200/_cluster/health?pretty"

# Lister les indices
Invoke-RestMethod -Uri "http://localhost:9200/_cat/indices?v"

# Rechercher des paiements
Invoke-RestMethod -Uri "http://localhost:9200/payments/_search?pretty" -Method Post -ContentType "application/json" -Body '{"query":{"match_all":{}}}'
```

### UI alternatives

| Outil | URL | Description |
|---|---|---|
| **Kibana** | http://localhost:5601 | Dashboard Elasticsearch (non inclus dans docker-compose) |
| **Elasticvue** | Extension Chrome/Firefox | Client Elasticsearch dans le navigateur |

## Comptes de démonstration

| Compte | Rôle | Organisation |
|---|---|---|
| `system.admin` / `Admin@123` | SYSTEM_ADMIN | — |

### Rôles disponibles à l'inscription

| Rôle | Description |
|---|---|
| `SUPPLIER_ADMIN` | Administrateur fournisseur |
| `SUPPLIER_AGENT` | Agent fournisseur |
| `SHOP_ADMIN` | Administrateur boutique |
| `SHOP_AGENT` | Agent boutique |

## Rôles & Permissions

| Rôle | Permissions |
|---|---|
| `SYSTEM_ADMIN` | Gérer organisations, utilisateurs, audit, stats |
| `SUPPLIER_ADMIN` | Gérer agents & paiements fournisseur |
| `SUPPLIER_AGENT` | Gérer paiements fournisseur |
| `SHOP_ADMIN` | Gérer agents, créer/annuler paiements boutique |
| `SHOP_AGENT` | Créer/annuler paiements boutique |

## API Gateway Routes

| Route | Service cible |
|---|---|
| `/api/auth/**` | Identity Service |
| `/api/users/**` | Identity Service |
| `/api/suppliers/{id}/agents/**` | Identity Service |
| `/api/organizations/**` | Organization Service |
| `/api/payments/**` | Payment Service |
| `/api/notifications/**` | Notification Service |

## Technologie

- **Backend** : Java 26, Spring Boot 4.1, Spring Cloud Gateway, Hibernate 7.4, Flyway, RabbitMQ
- **Frontend** : Angular 16, TypeScript
- **Base de données** : MySQL 8.4 (une base par microservice)
- **Recherche** : Elasticsearch 9.0.0 (indexation des paiements)
- **Infra** : Docker multi-stage, Docker Compose, Nginx
- **Architecture** : DDD, Clean/Hexagonal, Event-Driven, Outbox Pattern

## Documentation

> Index central avec état à jour / obsolète : [docs/README.md](docs/README.md).
> Routes front réelles (41 URLs) : [FRONTEND_ROUTES.md](FRONTEND_ROUTES.md).
> Compte-rendu d'acceptation (B/M) : [COMPTE_RENDU_EXPERT.md](COMPTE_RENDU_EXPERT.md).

| Sujet | Fichier |
|---|---|
| Architecture globale | [docs/architecture.md](docs/architecture.md) |
| DDD (aggregates, entities, VOs) | [docs/ddd.md](docs/ddd.md) |
| Bounded contexts | [docs/bounded-contexts.md](docs/bounded-contexts.md) |
| Règles métier | [docs/business-rules.md](docs/business-rules.md) |
| Modèle de données MySQL | [docs/database.md](docs/database.md) |
| API REST | [docs/api.md](docs/api.md) |
| Sécurité (JWT, RBAC) | [docs/security.md](docs/security.md) |
| Événements & Outbox | [docs/events.md](docs/events.md) |
| Déploiement & CI/CD | [docs/deployment.md](docs/deployment.md) |
| Guide développement local | [deploy/LOCAL_DEV.md](deploy/LOCAL_DEV.md) |
