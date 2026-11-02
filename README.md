# NEXTERA — Payment Platform

Plateforme B2B de gestion des relations fournisseurs / boutiques : catalogue et stocks, commandes et livraisons, paiements, facturation tunisienne (TVA/FODEC/timbre), retenues à la source TEJ, litiges et notifications temps réel.

## Documentation

| Document | Contenu |
|---|---|
| [Fonctionnalités](docs/FONCTIONNALITES.md) | Rôles, parcours métier, fiscalité, litiges, notifications |
| [Dossier technique](docs/TECHNIQUE.md) | Architecture, API, données, sécurité, **déploiement local et serveur (VM OVH, HTTPS, backups, CI/CD)** |

## Démarrage rapide (local)

Prérequis : Docker Desktop avec Docker Compose v2, Git.

```bash
cd deploy
cp .env.example .env
# Renseigner les secrets et adapter les valeurs locales, puis :
docker compose up --build -d
docker compose ps
```

Pour un accès HTTP local, `deploy/.env` doit contenir au minimum :

```dotenv
JWT_SECRET=<secret-aleatoire-32-octets-minimum>
INTERNAL_SECRET=<secret-aleatoire-32-caracteres-minimum>
MYSQL_ROOT_PASSWORD=<mot-de-passe>
MYSQL_PASSWORD=<mot-de-passe>
RABBITMQ_PASSWORD=<mot-de-passe>
AUTH_COOKIE_SECURE=false
CORS_ALLOWED_ORIGINS=http://localhost:8080
FRONTEND_URL=http://localhost:8080
WEBAUTHN_RP_ID=localhost
WEBAUTHN_ALLOWED_ORIGINS=http://localhost:8080
RATE_LIMIT_AUTH_PER_MINUTE=1000
```

| Accès | URL |
|---|---|
| Application | http://localhost:8080 |
| API Gateway | http://localhost:8081 |
| Adminer (MySQL) | http://localhost:8086 |
| RabbitMQ Management | http://localhost:15673 |
| Mailpit (emails) | http://localhost:8025 |

Le compte admin initial n'est pas créé par défaut : renseigner `SEED_ADMIN_ENABLED=true` et `SEED_ADMIN_PASSWORD` dans `deploy/.env` avant le premier démarrage, puis désactiver le seed.

## Déploiement serveur

Le déploiement est automatisé par GitHub Actions (images GHCR, publication TEST sur `release/**`, PROD sur `master`). La procédure complète — préparation de la VM, secrets, HTTPS/nginx, passkeys, backups, rollback — est dans [docs/TECHNIQUE.md § 9](docs/TECHNIQUE.md#9-déploiement-serveur-vm-ovh).

## Structure

```
backend/               services Spring Boot (gateway, identity, organization, payment, notification)
payment-platform-ui/   application Angular 21 / PWA
deploy/                docker compose, .env.example, nginx, scripts de backup
docs/                  documentation fonctionnelle et technique
```
