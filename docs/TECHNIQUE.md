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
- SSE (`/api/notifications/stream`) : nginx contourne le gateway (EventSource sans header) ; le JWT signé en query (`?token=`, HMAC + expiry vérifiés dans le contrôleur) fait foi, pas de session.
- Gateway : `JwtValidationFilter`, `IpWhitelistFilter` (CIDR, `172.16.0.0/12` en dev), `RateLimitFilter` (quotas stricts sur l'auth).
- Isolation inter-organisations vérifiée (tests d'accès croisés), journal d'audit, secrets jamais commités (voir `deploy/.env`).

## 6. Fiscalité — points d'implémentation

- `FiscalRules` : ordre remise ligne (montant = PU × Qté × % / 100) → FODEC 1 % dans base TVA → TVA par taux → TTC → timbre 1,000 DT ; aucune remise globale ; miroir frontend `tunisian-fiscal.ts`.
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
- Branching : `release/**` → publication **auto vers TEST** ; `master` → publication **auto vers PROD** ; `develop` → build + tests **sans déploiement**. Publication **manuelle** : Actions → Run workflow → `test` ou `prod` (déploie la branche sélectionnée).
- Déploiement : images **prébuildées en CI et poussées sur GHCR** (`test-latest` / `prod-latest`) ; les VM ne font que `pull` + `up -d` (**jamais de build**, pas de tests/sonar hors CI). Exécution root via `sudo -i`, `prune` auto (images pendantes + cache build) en fin de run, droits rendus à l'utilisateur SSH. Plafond heap JVM (`-Xmx384m`) pour la petite VM. Paquets GHCR publics (sinon `docker login ghcr.io` sur la VM).
### Mise en route déploiement (VM OVH) — pas à pas

VM TEST : `ubuntu@51.222.6.2` — **en service** (Docker 29, clone à jour, `.env` renseigné, clé CI installée). Déploiements CI exécutés **root** via `sudo -i` (droits rendus à l'utilisateur SSH en fin de run). Même procédure pour la PROD avec les secrets `OVH_PROD_*`.

1. **Clé SSH dédiée (sans passphrase, obligatoire pour la CI)** — sur ton poste :
   ```powershell
   ssh-keygen -t ed25519 -C "github-deploy-test" -f $env:USERPROFILE\.ssh\ovh_test_deploy -N '""'
   ```
   Si tu as déjà une clé qui se connecte à la VM, réutilise-la (affiche-la avec `type $env:USERPROFILE\.ssh\id_ed25519`).
2. **Autoriser la clé sur la VM** (clé publique → `authorized_keys`) :
   ```powershell
   type $env:USERPROFILE\.ssh\ovh_test_deploy.pub | ssh ubuntu@51.222.6.2 "mkdir -p ~/.ssh; cat >> ~/.ssh/authorized_keys; chmod 600 ~/.ssh/authorized_keys; chmod 700 ~/.ssh"
   ssh -i $env:USERPROFILE\.ssh\ovh_test_deploy ubuntu@51.222.6.2 "echo OK"
   ```
3. **Secrets GitHub** (repo → Settings → Secrets and variables → Actions) :
   `OVH_TEST_HOST`=`51.222.6.2`, `OVH_TEST_USER`=`ubuntu`, `OVH_TEST_SSH_KEY`=contenu complet de la clé **privée** (`type $env:USERPROFILE\.ssh\ovh_test_deploy`), optionnels : `OVH_TEST_PORT` (défaut 22), `OVH_TEST_DIR` (défaut `/opt/payment-platform`), `OVH_TEST_URL` (ex. `http://51.222.6.2:8081`, active le health check).
4. **Préparer la VM** (en SSH, une seule fois) :
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose-plugin git
   sudo mkdir -p /opt/payment-platform && sudo chown $USER /opt/payment-platform
   cd /opt/payment-platform && git clone <url-du-depot>.git .
   cp deploy/.env.example deploy/.env && nano deploy/.env   # mots de passe + secrets applicatifs
   ```
   Dépôt privé : ajoute une clé de déploiement (repo → Settings → Deploy keys) ou clone via token.
5. **Vérifier** : tout push sur `release/**` déclenche build + tests + déploiement auto sur la VM (suivi dans l'onglet Actions). Hôte en `.invalid` ou secrets absents = simulation verte (mode fake), jamais d'échec bloquant.
6. **Backup manuel** : Actions → workflow `Backup` → `test`/`prod` + tag. Restauration sur VM : `sh deploy/backup/restore.sh <fichier.sql.gz>` (confirmation `RESTORE`).

### Backups MySQL (PROD uniquement — pas de backup sur TEST)

- Service `db-backup` (image `mysql:8.4`, profil compose `backup` activé uniquement en PROD) : dump des 4 bases (`identity/organization/payment/notification_db`, `--single-transaction`, routines+events) **toutes les 8 h** (`BACKUP_INTERVAL_SECONDS`, défaut 28800), fichiers `AAAAMMJJ-HHMMSS-<tag>.sql.gz` dans `./backups/` (**gitignoré**), **rotation : suppression au-delà de 3 j** (`BACKUP_RETENTION_DAYS`).
- **Backup pré-prod automatique** : avant chaque déploiement PROD, la CI sauvegarde la base (`pre-prod-<timestamp>`) puis déploie.
- **Backup manuel** : workflow `Backup` (Actions → Run workflow → `test`/`prod` + tag, via `--profile backup`), ou sur la VM PROD : `docker compose --profile backup exec db-backup /scripts/backup-now.sh [tag]`.
- **Commandes toutes prêtes** (VM PROD, depuis la racine du repo) :
  - Backup database daté : `sh deploy/backup/backup.sh db` → tag `db-AAAAMMJJ-HHMMSS`
  - Backup release : `sh deploy/backup/backup.sh release 1.1.0` → tag `release-1.1.0`
- **Restauration** (sur la VM, depuis la racine du repo) : `sh deploy/backup/restore.sh <fichier.sql.gz>` (confirmation `RESTORE` requise, écrase les données). Scripts : `deploy/backup/` (`backup-now.sh`, `backup-loop.sh`, `restore.sh`, `backup.sh`).
- Ménage manuel VM : `docker image prune -a -f`, `docker builder prune -af`, `docker system df`. **Jamais** `prune --volumes` (données MySQL).

## 9. Versioning (SemVer)

- Format `MAJEUR.MINEUR.PATCH` (ex. `1.2.0`) : MAJEUR = rupture, MINEUR = fonctionnalité, PATCH = correctif.
- Sources uniques, bumpées **ensemble** à chaque release : `backend/pom.xml` (`<version>`), `payment-platform-ui/package.json` (`"version"`), `sonar.projectVersion` (`sonar-project.properties`). Version actuelle : `1.1.0` (release en cours).
- Branches :
  - `master` : production, toujours déployable. Chaque merge de release = **tag `vX.Y.Z`** (ex. `git tag v1.1.0`).
  - `develop` : intégration (build + tests, jamais déployée). Après une release, elle porte la version suivante en `-SNAPSHOT` côté backend (ex. `1.2.0-SNAPSHOT`).
  - `release/X.Y.Z` : coupée depuis `develop` pour stabiliser (correctifs uniquement) → **déploiement auto vers TEST** à chaque push → merge vers `master` (+ tag) et back-merge vers `develop`.
  - `hotfix/X.Y.Z` : coupée depuis `master` pour un correctif prod urgent → merge vers `master` (+ tag PATCH) et back-merge vers `develop`.
- Commandes types :
  - `git checkout -b release/1.1.0 develop` (puis bumper les 3 versions à `1.1.0`)
  - `git checkout master && git merge --no-ff release/1.1.0 && git tag v1.1.0 && git push origin master --tags`
  - `git checkout develop && git merge --no-ff release/1.1.0 && git push origin develop`
- Observabilité : logs structurés, correlation-id, healthchecks Spring, console RabbitMQ/Adminer/Mailpit en dev.
