# Payment Platform — Dossier technique

> Référence technique unique du projet (consolidation des anciens docs `docs/`).
> Le fonctionnel est décrit dans [FONCTIONNALITES.md](FONCTIONNALITES.md).

## 1. Stack

| Couche | Technologie |
|---|---|
| Frontend | Angular 21, TypeScript 5.9, ngx-translate (FR/EN/AR + RTL), Firebase (push FCM), angularx-qrcode, pdfjs-dist, PWA |
| Backend | Java 26, Spring Boot 4.1.0, Spring Cloud Gateway (WebMVC), Spring Data JPA, Spring AMQP, Spring Mail |
| Base de données | MySQL 8.4 (1 base par service), H2 pour les tests |
| Messagerie | RabbitMQ 4 (events via **outbox transactionnel**, relais `@Scheduled`) |
| Cache / outils | Redis 7, Adminer, Mailpit |
| Build / qualité | Maven (multi-modules), npm, Karma/Jasmine, Cypress, Sonar |
| Déploiement | Docker Compose (11 conteneurs) |

## 2. Architecture

```
Angular (nginx :8080, dev :4200)
   │ REST + SSE
   ▼
API Gateway :8081 (JWT + RBAC + CORS + rate-limit + IP whitelist + correlation-id)
   ├── Identity :8082 ──────▶ identity_db
   ├── Organization :8083 ──▶ organization_db (commandes, stocks, catalogue, certificats RAS, TEIF)
   ├── Payment :8084 ───────▶ payment_db
   └── Notification :8085 ──▶ notification_db
RabbitMQ (amqp 5673 / mgmt 15673), MySQL (hôte 3307), Redis, Mailpit, Adminer
```

### Principes
- **Une base par service**, aucun accès croisé aux tables.
- Synchrone : REST uniquement via le Gateway (ex. Payment → Organization pour valider une relation).
- Asynchrone : **domain events** + outbox idempotent (pas de transaction distribuée).
- DDD par service : `domain` (modèle, règles) / `application` (usecases, DTO) / `infrastructure` (JPA, messaging, HTTP) / `interfaces` (REST).
- Le Gateway n'est pas une frontière de confiance : chaque service **re-valide le JWT et l'état du compte**.

### Événements principaux
`OrderCreated/Confirmed/Delivered/Accepted/Cancelled`, `LowStockAlert`, `WithholdingDueReminder` (rappel J-7),
`PaymentCreated/Confirmed/Rejected/Cancelled`, `UserCreated/Disabled`, activations/désactivations fournisseur/boutique.
Consommateur principal : notification-service (SSE + push FCM).

## 3. API (via Gateway, préfixe `/api`)

| Domaine | Exemples d'endpoints |
|---|---|
| Auth (`/auth`) | `POST /login|/refresh|/logout|/register`, `GET /me`, `POST /change-password`, reset (`/request|/validate|/confirm`), setup (`/validate|/complete`) |
| Utilisateurs (`/users`) | CRUD, `PATCH /{id}/activate|/disable`, agents (`/agents`), `PATCH /me/preferences` |
| Admin orgs (`/admin`) | CRUD fournisseurs/boutiques, `PATCH …/activate|/disable|/fiscal-profile`, `GET /stats` |
| Commandes (`/orders`) | CRUD, cycle `confirm→prepare→ready→assign→accept-delivery→confirm-delivery→deliver→accept`, `reorder`, `reject`, géolocalisation, commentaires, `export/csv`, `/{id}/invoice` (PDF), `/{id}/teif.xml` (TEIF v1.8.8), `PATCH /{id}/external-reference`, certificats RAS (`GET|/pdf|/xml`, `PATCH …/deposit`, `/batch`, `/due`, `/monthly-summary`) || Stocks (`/stock`, `/catalog`) | produits CRUD + image, mouvements, `low-stock-alerts`, optimisation (`GET|/configure`), familles/catégories |
| Soldes (`/balances`) | soldes fournisseur/boutique, par couple, `POST /adjust` |
| Paiements (`/payments`) | CRUD, `confirm|reject|cancel`, `/stats`, `/overdue`, `/agent-summary`, `/supplier-summary`, exports, facture, auto-création interne |
| Litiges (`/disputes`) | création, par commande, messages, résolution |
| Notifications (`/notifications`) | liste, `unread-count`, `read-all`, `/{id}/read`, flux **SSE** `/stream`, tokens FCM |
| Rapports (`/reports`) | financier fournisseur ; audit (`/audit-logs`) |

## 4. Données (MySQL, migrations versionnées)

- **identity_db** : utilisateurs, rôles, agents, préférences, tokens reset/setup.
- **organization_db** : organisations (+ profil fiscal : MF, patente, identifiant TEJ, nature de retenue par défaut, adresse, régime, attestation), relations, produits (+ TVA/FODEC/TTC), stocks + mouvements, commandes + articles (snapshots fiscaux, ventilation TVA, timbre, référence externe), certificats de retenue, litiges + messages, soldes, audit.
- **payment_db** : paiements (+ ASAP, échéances), factures de paiement.
- **notification_db** : notifications, tokens FCM.
- Règles : montants **3 décimales**, MF format `1234567A/A/M/000`, statuts par chaîne contrôlée, records Java = constructeurs positionnels.

## 5. Sécurité

- JWT HS256 (secret via env, expiration 30 min), re-validation par service, contrôle RBAC par endpoint (`@PreAuthorize`).
- Gateway : `JwtValidationFilter`, `IpWhitelistFilter` (CIDR, `172.16.0.0/12` en dev), `RateLimitFilter` (quotas stricts sur l'auth).
- Isolation inter-organisations vérifiée (tests d'accès croisés), journal d'audit, secrets jamais commités (voir `deploy/.env`).

## 6. Fiscalité — points d'implémentation

- `FiscalRules` : ordre remise → FODEC 1 % dans base TVA → TVA par taux → TTC → timbre 1,000 DT ; miroir frontend `tunisian-fiscal.ts`.
- `TeifGeneratorService` : TEIF v1.8.8 (MF 13 chars, jjMMAA, codes I-*, timbre séparé, `RefTtnVal`), `GET /{id}/teif.xml`.
- `Withholding*` : certificats rattachés aux factures, XML/loo PDF+QR, lot mensuel, J-7 planifié (`WithholdingDueScheduler`), synthèse mensuelle.
- Référence e-facture : `docs/SPEC_EFACTURE_TTN.md` (archivé dans `archive-docs/`).

## 7. Tests

- Backend : JUnit 5 + AssertJ, SpringBootTest **H2** (zéro simulation sur les parcours critiques), ArchUnit (règles DDD).
- Frontend : Karma/Jasmine (`npm run test:ci`, navigateur `ChromeHeadlessCI`), specs composants + services.
- E2E : Cypress (`cypress.config.dev.ts` : app `http://localhost:8080`, API `http://localhost:8081`) — **lancement uniquement sur feu vert explicite**.
- Niveaux : `mvn verify` par module, `npm run test:ci`, `tsc` Cypress, e2e dev.

## 8. Exploitation

- Local : `deploy/` (compose + `.env` + scripts `e2e-local/e2e-dev/stop`) ; dev direct : Angular `:4200`, services `:8081-:8085`.
- CI (`.github/workflows/ci.yml`) : build, tests backend/frontend, Sonar, images Docker, budgets CSS (16 ko). **Pas d'e2e en CI** (lancé uniquement en local sur feu vert).
- Branching : `release/**` → publication **auto vers TEST** ; `main` → publication **auto vers PROD** ; `develop` → build + tests **sans déploiement**. Publication **manuelle** : Actions → Run workflow → `test` ou `prod` (déploie la branche sélectionnée).
- Déploiement sur **VM OVH** (SSH via `appleboy/ssh-action`) : `git pull` de la branche puis `docker compose up -d --build` sur la VM (aucun registre requis). Secrets : `OVH_TEST_HOST/USER/SSH_KEY` (+ `PORT`, `DIR`, `URL` optionnels), idem `OVH_PROD_*`. Préparer sur chaque VM : clone dans `/opt/payment-platform` (ou `OVH_*_DIR`), `deploy/.env` renseigné (modèle : `deploy/.env.example`), accès git au dépôt (clé de déploiement si privé), Docker installé.

## 9. Versioning (SemVer)

- Format `MAJEUR.MINEUR.PATCH` (ex. `1.2.0`) : MAJEUR = rupture, MINEUR = fonctionnalité, PATCH = correctif.
- Sources uniques, bumpées **ensemble** à chaque release : `backend/pom.xml` (`<version>`), `payment-platform-ui/package.json` (`"version"`), `sonar.projectVersion` (`sonar-project.properties`). Version actuelle : `1.0.0`.
- Branches :
  - `main` : production, toujours déployable. Chaque merge de release = **tag `vX.Y.Z`** (ex. `git tag v1.1.0`).
  - `develop` : intégration (build + tests, jamais déployée). Après une release, elle porte la version suivante en `-SNAPSHOT` côté backend (ex. `1.2.0-SNAPSHOT`).
  - `release/X.Y.Z` : coupée depuis `develop` pour stabiliser (correctifs uniquement) → **déploiement auto vers TEST** à chaque push → merge vers `main` (+ tag) et back-merge vers `develop`.
  - `hotfix/X.Y.Z` : coupée depuis `main` pour un correctif prod urgent → merge vers `main` (+ tag PATCH) et back-merge vers `develop`.
- Commandes types :
  - `git checkout -b release/1.1.0 develop` (puis bumper les 3 versions à `1.1.0`)
  - `git checkout main && git merge --no-ff release/1.1.0 && git tag v1.1.0 && git push origin main --tags`
  - `git checkout develop && git merge --no-ff release/1.1.0 && git push origin develop`
- Observabilité : logs structurés, correlation-id, healthchecks Spring, console RabbitMQ/Adminer/Mailpit en dev.
