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

## URLs & Ports

| Service | URL | Description |
|---|---|---|
| **Angular App** | http://localhost:8080 | Frontend SPA |
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
