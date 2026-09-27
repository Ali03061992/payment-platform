# Guide d'utilisation des tests E2E Cypress

> État vérifié 25/09/2026 : **19 specs** (`01-auth` → `19-delivery-roles`),
> **246 blocs `it(` comptés statiquement** (contexte mission : 244/244 verts en run
> complet — écart de 2 à confirmer par run ; aucun `it.skip/only`).
> Configs : `cypress.config.ts` (baseUrl `http://localhost:4200`, apiUrl `http://localhost:8081`),
> `cypress.config.local.ts`, `cypress.config.dev.ts`.

## Architecture des tests

```
cypress/
├── e2e/
│   ├── 01-auth.cy.ts                    # Authentification (login, register, logout, guards) — incl. M5 (compte naît DISABLED) + /404
│   ├── 02-admin-dashboard.cy.ts         # Dashboard administrateur
│   ├── 03-admin-users.cy.ts             # Gestion des utilisateurs (paginée B5)
│   ├── 04-admin-organizations.cy.ts     # Fournisseurs, boutiques, relations
│   ├── 05-supplier-products.cy.ts       # Catalogue produits fournisseur
│   ├── 06-supplier-stock.cy.ts          # Gestion du stock
│   ├── 07-supplier-orders.cy.ts         # Commandes fournisseur + livraisons
│   ├── 08-shop-orders.cy.ts             # Commandes boutique + balance
│   ├── 09-payments.cy.ts                # Paiements (liste, création idempotente B1, détail, stats)
│   ├── 10-notifications.cy.ts           # Notifications (UI + API)
│   ├── 11-navigation-rbac.cy.ts         # Navigation + RBAC (/403 RoleGuard)
│   ├── 12-api-integration.cy.ts         # API complets — incl. M1 refresh/logout (l.52-79)
│   ├── 13-stock-optimization.cy.ts      # Optimisation stock
│   ├── 14-delivery-workflow.cy.ts       # Workflow livraison complet
│   ├── 15-rate-limit-auth.cy.ts         # B2 : headers X-RateLimit-* (pas de hammering, cf. budget ci-dessous)
│   ├── 16-gateway-security.cy.ts        # B3 : 401 JSON gateway (deny-by-default, internal non proxied)
│   ├── 17-pagination-aggregates.cy.ts   # B5 : enveloppes + supplier-summary SQL (deltas)
│   ├── 18-asap-payment.cy.ts            # ASAP : 1 seul paiement auto (asap-<orderId>)
│   └── 19-delivery-roles.cy.ts          # Livraison : admin fournisseur livre, SHOP_ADMIN destinataire (UI+API)
├── fixtures/
│   └── users.json                       # Utilisateurs de test
├── support/
│   ├── commands.ts                      # Commandes personnalisées
│   └── e2e.ts                           # Support file
└── screenshots/                         # Screenshots automatiques en cas d'échec
```

## Prérequis

- Node.js 18+
- Docker + Docker Compose
- Chrome ou Edge (pour Cypress)

## Comment lancer l'application

### Option 1 : Docker Compose (recommandé)

```powershell
# Depuis la racine du projet
cd deploy

# Démarrer tous les services
docker compose up -d

# Attendre ~30s que les services démarrent
# Vérifier que tout est up :
docker compose ps
```

**Services démarrés :**
| Service | URL | Port |
|---------|-----|------|
| Angular App | http://localhost:8080 | 8080 |
| API Gateway | http://localhost:8081 | 8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html | 8081 |
| Adminer (MySQL) | http://localhost:8086 | 8086 |
| RabbitMQ Mgmt | http://localhost:15673 | 15673 |

### Option 2 : Arrêter les services

```powershell
cd deploy
docker compose down -v
```

## Comment lancer les tests Cypress

### Depuis payment-platform-ui/

```powershell
cd payment-platform-ui
```

### 1. Vérifier que l'app tourne

```powershell
# Vérifier que le frontend est accessible
curl http://localhost:8080

# Vérifier que l'API répond
curl http://localhost:8081/api/auth/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"system.admin\",\"password\":\"Admin@123\"}"
```

### 2. Lancer tous les tests (headless)

```powershell
npx cypress run --browser chrome --headless
```

### 3. Lancer un fichier spécifique

```powershell
# Auth uniquement
npx cypress run --spec "cypress/e2e/01-auth.cy.ts" --browser chrome --headless

# API uniquement
npx cypress run --spec "cypress/e2e/12-api-integration.cy.ts" --browser chrome --headless

# RBAC uniquement
npx cypress run --spec "cypress/e2e/11-navigation-rbac.cy.ts" --browser chrome --headless
```

### 4. Ouvrir Cypress en mode interactif

```powershell
npx cypress open
```

### 5. Commandes npm rapides

```powershell
npm run cy:run           # Tous les tests headless
npm run cy:open          # Mode interactif
npm run e2e              # Chrome headless
npm run cy:run:local     # Config locale (cypress.config.local.ts, baseUrl 4200)
npm run e2e:local        # Idem
```

### Budget rate-limit E2E (B2)

La suite fait ~70 logins depuis une seule IP : `RATE_LIMIT_AUTH_PER_MINUTE=1000`
en CI (`.github/workflows/ci.yml`) et en local (`application-local.yml`,
`.run/5_API_Gateway.run.xml`). Le 429 prod (10/min + `Retry-After: 60`) est prouvé
par `RateLimitFilterTest` ; la spec 15 vérifie les headers `X-RateLimit-*`
sans hammering (sinon suite flaky). Vérification manuelle du 429 :
boucler 11× `POST $GW/api/auth/login` avec le défaut prod.

## Utilisateurs de test

| Username | Password | Rôle | Organisation |
|----------|----------|------|-------------|
| `system.admin` | `Admin@123` | SYSTEM_ADMIN | - |
| `covale.admin` | `Admin@123` | SUPPLIER_ADMIN | Covale (org 1) |
| `pointteck.admin` | `Admin@123` | SUPPLIER_ADMIN | Pointteck (org 2) |
| `covale.agent1` | `Admin@123` | SUPPLIER_AGENT | Covale (org 1) |
| `abdelslam` | `Admin@123` | SHOP_ADMIN | Tunis Sousse (org 3) |
| `ali` | `Admin@123` | SHOP_ADMIN | Sfax Mahdiya (org 4) |

## Couverture des tests

| Fichier | `it(` statiques | Ce qui est testé |
|---------|-------|------------------|
| 01-auth | 14 | Login, register (M5 DISABLED), logout, guards, /404 |
| 02-admin-dashboard | 7 | Stats, profil, actions rapides, sidebar |
| 03-admin-users | 10 | CRUD utilisateurs, filtres, activation/désactivation (B5) |
| 04-admin-organizations | 17 | Fournisseurs, boutiques, relations, stats org |
| 05-supplier-products | 8 | Catalogue produits, création, filtres |
| 06-supplier-stock | 10 | Gestion stock, quantités, statuts, dashboard |
| 07-supplier-orders | 15 | Commandes, livraisons, paiements agents |
| 08-shop-orders | 17 | Commandes boutique, création, balance |
| 09-payments | 23 | Paiements liste, création idempotente (B1), détail, QR, stats |
| 10-notifications | 12 | UI notifications, API, triggering paiements |
| 11-navigation-rbac | 30 | Navigation complète, RBAC par rôle (/403) |
| 12-api-integration | 28 | API auth, refresh/logout (M1), admin, payments, orders, RBAC |
| 13-stock-optimization | 19 | Optimisation stock |
| 14-delivery-workflow | 26 | Workflow livraison complet |
| 15-rate-limit-auth | 2 | B2 headers rate-limit |
| 16-gateway-security | 3 | B3 401 gateway |
| 17-pagination-aggregates | 3 | B5 enveloppes + summary SQL |
| 18-asap-payment | 1 | ASAP auto-paiement unique |
| 19-delivery-roles | 1 | Livraison multi-rôles |
| **Total statique** | **246** | **19 specs** (contexte mission : 244/244 verts en run complet) |

## Résultats attendus

> L'ancien tableau (12 specs / 183 tests) est obsolète : la suite compte désormais
> **19 specs**. Le décompte statique donne 246 `it(` ; le run complet de référence
> annonce 244/244 verts (écart de 2 à confirmer par run, p. ex. tests conditionnels
> ou retries à 0 — `retries.runMode: 0`).

```
Spec                           Tests  Passing  Failing  Pending  Skipped
01-auth.cy.ts                    14       14        -        -        -
02-admin-dashboard.cy.ts          7        7        -        -        -
03-admin-users.cy.ts             10       10        -        -        -
04-admin-organizations.cy.ts     17       17        -        -        -
05-supplier-products.cy.ts        8        8        -        -        -
06-supplier-stock.cy.ts          10       10        -        -        -
07-supplier-orders.cy.ts         13       13        -        -        -
08-shop-orders.cy.ts             10       10        -        -        -
09-payments.cy.ts                19       19        -        -        -
10-notifications.cy.ts           12       12        -        -        -
11-navigation-rbac.cy.ts         29       29        -        -        -
12-api-integration.cy.ts         34       34        -        -        -
─────────────────────────────────────────────────────────────────────────
Total                           183      183        0        0        0
```

## Dépannage

### Les tests échouent sur "ECONNREFUSED"

L'application n'est pas démarrée. Lancez `docker compose up -d` depuis `deploy/`.

### Les tests échouent sur les sélecteurs

Vérifiez que le frontend est bien construit. Si vous avez modifié des templates Angular :

```powershell
cd payment-platform-ui
npx ng build
```

### Erreur de timeout

Augmentez le timeout dans `cypress.config.ts` :

```typescript
defaultCommandTimeout: 15000,  // défaut : 10000
requestTimeout: 15000,
responseTimeout: 60000,
```

### Nettoyer les screenshots

```powershell
Remove-Item -Recurse -Force cypress/screenshots/*
```
