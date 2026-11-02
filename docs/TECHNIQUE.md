# NEXTERA — Dossier technique

> Référence technique unique du projet.
> Le fonctionnel est décrit dans [FONCTIONNALITES.md](FONCTIONNALITES.md).
> Les étapes de déploiement local et serveur sont détaillées aux sections 8 et 9.

## 1. Stack

| Couche | Technologie |
|---|---|
| Frontend | Angular 21, TypeScript 5.9, ngx-translate (FR/EN/AR + RTL), Firebase (push FCM), angularx-qrcode, pdfjs-dist, PWA |
| Backend | Java 26, Spring Boot 4.1.0, Spring Cloud Gateway (WebMVC), Spring Data JPA, Spring AMQP, Spring Mail |
| Base de données | MySQL 8.4 (1 base par service), H2 pour les tests |
| Messagerie | RabbitMQ 4 (events via **outbox transactionnel**, relais `@Scheduled`) |
| Cache / outils | Redis 7, Adminer, Mailpit |
| Build / qualité | Maven (multi-modules), npm, Karma/Jasmine, Cypress, Sonar |
| Déploiement | Docker Compose (11 conteneurs), nginx hôte (TLS), GitHub Actions + GHCR |

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
- Asynchrone : **domain events** + outbox idempotent (pas de transaction distribuée). `OutboxRelay` relit toutes les secondes (`app.outbox.relay-interval-ms`, défaut 1000 ms) les 100 premiers événements non traités et les publie vers `payment.events`, `organization.events` ou `identity.events` ; l'`eventId` unique assure l'idempotence côté consommateur.
- DDD par service : `domain` (modèle, règles) / `application` (usecases, DTO) / `infrastructure` (JPA, messaging, HTTP) / `interfaces` (REST).
- Le Gateway n'est pas une frontière de confiance : chaque service **re-valide le JWT et l'état du compte**.

### Événements principaux
`OrderCreated/Confirmed/Delivered/Accepted/Cancelled`, `LowStockAlert`, `WithholdingDueReminder` (rappel J-7),
`PaymentCreated/Confirmed/Rejected/Cancelled`, `UserCreated/Disabled`, activations/désactivations fournisseur/boutique.
Consommateur principal : notification-service (SSE + push FCM).

## 3. API (via Gateway, préfixe `/api`)

| Domaine | Exemples d'endpoints |
|---|---|
| Auth (`/auth`) | `POST /login|/refresh|/logout`, `GET /me`, `POST /change-password`, reset (`/request|/validate|/confirm`), setup (`/validate|/complete`). Compte créé uniquement par un admin (`POST /api/users`, invitation email). |
| Passkeys (`/auth/webauthn`) | `register/options`, `register`, `credentials` (liste/renommage/révocation), `login/options`, `login` (publics et rate-limités). |
| Utilisateurs (`/users`) | CRUD, `PATCH /{id}/activate|/disable`, agents (`/agents`), `PATCH /me/preferences` |
| Admin orgs (`/admin`) | CRUD fournisseurs/boutiques, `PATCH …/activate|/disable|/fiscal-profile`, `GET /stats` |
| Commandes (`/orders`) | CRUD, cycle `confirm→prepare→ready→assign→accept-delivery→confirm-delivery→deliver→accept`, `reorder`, `reject`, géolocalisation, commentaires, `export/csv`, `/{id}/invoice` (PDF), `/{id}/teif.xml` (TEIF v1.8.8), `PATCH /{id}/external-reference`, certificats RAS (`GET|/pdf|/xml`, `PATCH …/deposit`, `/batch`, `/due`, `/monthly-summary`) |
| Stocks (`/stock`, `/catalog`) | produits CRUD + image, mouvements, `low-stock-alerts`, optimisation (`GET /api/suppliers/{supplierId}/optimization`, `POST …/configure`), familles/catégories |
| Soldes (`/balances`) | soldes fournisseur/boutique, par couple, `POST /adjust` |
| Paiements (`/payments`) | CRUD, `confirm|reject|cancel`, `/stats`, `/overdue`, `/agent-summary`, `/supplier-summary`, exports, facture, auto-création interne |
| Litiges (`/disputes`) | création (priorité/SLA), liste héritée + `/paged` (filtres statut/priorité/recherche), par commande, messages, `transition`, `priority`, `assign`, `notes`, pièces jointes (upload/download), résolution |
| Notifications (`/notifications`, `/push`) | liste, `unread-count`, `read-all`, `/{id}/read`, flux **SSE** `/stream`, tokens FCM, abonnements Web Push, préférences |
| Rapports (`/reports`) | financier fournisseur ; audit (`/audit-logs`) |

## 4. Données (MySQL, migrations versionnées)

- **identity_db** : utilisateurs, rôles, agents, préférences, tokens reset/setup, refresh tokens (hash SHA-256 uniquement), credentials et challenges WebAuthn.
- **organization_db** : organisations (+ profil fiscal : MF, patente, identifiant TEJ, nature de retenue par défaut, adresse, régime, attestation), relations, produits (+ TVA/FODEC/TTC), stocks + mouvements, commandes + articles (snapshots fiscaux, ventilation TVA, timbre, référence externe), certificats de retenue, litiges + messages, soldes, audit.
- **payment_db** : paiements (+ ASAP, échéances), factures de paiement.
- **notification_db** : notifications, tokens FCM, abonnements Web Push, préférences, clés VAPID.
- Règles : montants **3 décimales**, MF format `1234567A/A/M/000`, statuts par chaîne contrôlée, records Java = constructeurs positionnels.

### Optimisation des stocks
`StockOptimizationService` (organization-service) produit, par fournisseur, une classification ABC/XYZ, des prévisions de demande, le stock de sécurité, le point de commande, l'EOQ, les risques, anomalies et recommandations. La demande est dérivée des mouvements `OUT` ; les paramètres (délai fournisseur 7 j, coût de commande 50, coût de possession 25 %) sont modifiables via `POST …/optimization/configure` et conservés **en mémoire** (perdus au redémarrage, non partagés entre instances).

## 5. Sécurité

- JWT HS256 (secret via env, expiration 30 min), claims `sub`, `username`, `roles`, `organizationId`, `jti` ; re-validation par service, contrôle RBAC par endpoint (`@PreAuthorize`). En prod, `JWT_SECRET` est obligatoire (échec au démarrage si absent) et une valeur de dev connue est refusée.
- Refresh tokens opaques 256 bits : seul le SHA-256 est stocké, rotation à chaque usage (ancien révoqué avec traçabilité `replaced_by`), TTL 7 j, révocation au logout ou si le compte est désactivé. Le web utilise le cookie `pp_rt` (HttpOnly, Secure par défaut, SameSite=Strict, Path=`/api/auth`) ; le mobile envoie le token dans le body.
- SSE (`/api/notifications/stream`) : nginx contourne le gateway (EventSource sans header) ; le JWT signé en query (`?token=`, HMAC + expiry vérifiés dans le contrôleur) fait foi, pas de session.
- Gateway : `JwtValidationFilter`, `IpWhitelistFilter` (CIDR, `172.16.0.0/12` en dev), `RateLimitFilter` (quotas stricts sur l'auth).
- Isolation inter-organisations vérifiée (tests d'accès croisés), journal d'audit, secrets jamais commités (voir `deploy/.env`).
- **Passkeys (WebAuthn)** : challenges à usage unique stockés en base (TTL 300 s), endpoints de connexion publics mais rate-limités, enregistrement et gestion réservés aux utilisateurs authentifiés. Désactivées côté UI par défaut (`PASSKEYS_ENABLED = false` dans `payment-platform-ui/src/app/core/passkeys.config.ts`).
- **Web Push PWA** : abonnements `PushManager`, clés VAPID persistées ou fournies par env, purge automatique des endpoints expirés (404/410). Nécessite HTTPS et un build de production (service worker actif).

## 6. Fiscalité — points d'implémentation

- Création/édition d'organisation (`CreateOrganizationRequest`) : `matriculeFiscal` obligatoire sauf particulier/export (format `1234567A/A/M/000`), `fiscalRegime` parmi ASSUJETTI (défaut), FORFAITAIRE, SUSPENDU, EXPORT, NON_ASSUJETTI, ETAT_PUBLIC ; attestation de suspension obligatoire si SUSPENDU ; `tejIdentifier` et `defaultWithholdingNature` (ex. HONORAIRES_REEL).
- `FiscalRules` : ordre remise ligne (montant = PU × Qté × % / 100) → FODEC 1 % dans base TVA → TVA par taux → TTC → timbre 1,000 DT ; aucune remise globale ; miroir frontend `tunisian-fiscal.ts`.
- `TeifGeneratorService` : TEIF v1.8.8 (MF 13 chars, jjMMAA, codes I-*, timbre séparé, `RefTtnVal`), `GET /{id}/teif.xml`.
- `Withholding*` : certificats rattachés aux factures, XML/PDF+QR, lot mensuel, J-7 planifié (`WithholdingDueScheduler`), synthèse mensuelle.

## 7. Tests

- Backend : JUnit 5 + AssertJ, SpringBootTest **H2** (zéro simulation sur les parcours critiques), ArchUnit (règles DDD).
- Frontend : Karma/Jasmine (`npm run test:ci`, navigateur `ChromeHeadlessCI`), specs composants + services.
- E2E : Cypress (`cypress.config.dev.ts` : app `http://localhost:8080`, API `http://localhost:8081`) — **lancement uniquement sur feu vert explicite**.
- Niveaux : `mvn verify` par module, `npm run test:ci`, `tsc` Cypress, e2e dev.

## 8. Déploiement local

### 8.1 Prérequis

| Outil | Usage |
|---|---|
| Docker Desktop + Docker Compose v2 | stack complète ou infrastructure seule |
| Git | clone du dépôt |
| JDK 26 + wrapper Maven (`backend/mvnw`) | exécution des services hors Docker |
| Node.js 22 + npm | frontend Angular, builds et tests |
| Ports libres | `8080`, `8081`, `8086`, `8025`, `3307`, `5673`, `15673`, `6379` (IDE : `4200`, `8082-8085`) |

### 8.2 Démarrage complet avec Docker

```bash
cd deploy
cp .env.example .env
# Éditer .env (voir 8.5), puis :
docker compose up --build -d
docker compose ps
docker compose logs -f
```

Premier démarrage : compter plusieurs minutes (build Maven/Angular + initialisation MySQL). Les bases sont créées par `deploy/mysql-init/01-init-databases.sql`.

| Service | URL locale |
|---|---|
| Application Angular | http://localhost:8080 |
| API Gateway | http://localhost:8081 (`/actuator/health`) |
| Swagger (via gateway) | http://localhost:8081/api/auth/swagger-ui.html |
| Adminer (MySQL) | http://localhost:8086 (serveur `mysql`) |
| RabbitMQ Management | http://localhost:15673 |
| Mailpit (emails) | http://localhost:8025 |

| Infrastructure | Port hôte | Identifiants par défaut |
|---|---|---|
| MySQL 8.4 | `127.0.0.1:3307` | `payment_app` / valeur de `MYSQL_PASSWORD` |
| MySQL root | `127.0.0.1:3307` | `root` / valeur de `MYSQL_ROOT_PASSWORD` |
| RabbitMQ AMQP | `127.0.0.1:5673` | `RABBITMQ_USER` / `RABBITMQ_PASSWORD` |
| RabbitMQ Management | `127.0.0.1:15673` | idem |
| Redis | `127.0.0.1:6379` | — |

> Les services backend et l'application ne sont pas exposés sur toutes les interfaces : le compose les publie sur `127.0.0.1` (accès local ou tunnel SSH en serveur).

Compte initial : aucun mot de passe par défaut. Pour créer l'admin système au premier démarrage, renseigner `SEED_ADMIN_ENABLED=true` et `SEED_ADMIN_PASSWORD` dans `deploy/.env`, puis redémarrer `identity-service`. Désactiver le seed ensuite.

### 8.3 Développement IDE (infrastructure Docker + code local)

```bash
cd deploy
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps
```

Puis lancer, avec le profil Spring `local` :
- `identity-service` (8082), `organization-service` (8083), `payment-service` (8084), `notification-service` (8085), `api-gateway` (8081) — depuis l'IDE ou avec `backend/mvnw -pl <service> spring-boot:run -Dspring-boot.run.profiles=local`.
- Frontend : `cd payment-platform-ui && npm ci && npm run start:local` → http://localhost:4200.

Le profil `local` utilise `RATE_LIMIT_AUTH_PER_MINUTE=1000` pour ne pas bloquer les tests E2E (prod = 10). Rebuild ciblé : `cd backend && ./mvnw -pl <service> -am package -DskipTests` (Windows : `.\mvnw.cmd`), ou `docker compose up --build -d <service>`.

### 8.4 Arrêt, reset et dépannage

```bash
cd deploy
docker compose down                  # arrêt, données conservées
docker compose down -v               # reset complet (supprime les bases)
docker compose down -v --rmi local   # reset + images locales
docker compose restart identity-service
docker compose up --build -d identity-service
docker compose logs -f <service>
```

- MySQL lent : `docker compose up mysql -d`, attendre `healthy`, puis `docker compose up -d`.
- Connexion MySQL : `docker compose exec mysql mysql -u payment_app -p identity_db`.
- Files RabbitMQ : `docker compose exec rabbitmq rabbitmqctl list_queues`.
- **Ne jamais** exécuter `docker volume prune` ou `down -v` sans backup : les 4 bases sont dans le volume `mysql-data`.

### 8.5 Variables d'environnement locales

Fichier : `deploy/.env` (jamais commité ; modèle `deploy/.env.example`).

| Variable | Valeur locale type | Rôle |
|---|---|---|
| `JWT_SECRET` | chaîne aléatoire ≥ 32 octets | signature JWT |
| `JWT_EXPIRATION` | `30m` | durée de vie access token |
| `REFRESH_TOKEN_TTL_DAYS` | `7` | durée refresh token |
| `AUTH_COOKIE_SECURE` | `false` en HTTP local | cookie `pp_rt` |
| `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD` | secrets locaux | MySQL |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | secrets locaux | RabbitMQ |
| `INTERNAL_SECRET` | chaîne aléatoire ≥ 32 caractères | appels inter-services |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8080` | origines autorisées |
| `FRONTEND_URL` | `http://localhost:8080` | liens emails / origine passkeys |
| `WEBAUTHN_RP_ID` | `localhost` | RP ID (sans schéma ni port) |
| `WEBAUTHN_ALLOWED_ORIGINS` | `http://localhost:8080` | origines WebAuthn |
| `RATE_LIMIT_AUTH_PER_MINUTE` | `1000` en local | quota auth |
| `ALLOWED_IPS` | `127.0.0.1,0:0:0:0:0:0:0:1,172.16.0.0/12` | whitelist gateway |
| `SMTP_*` | vides → Mailpit | emails |
| `FIREBASE_*`, `FCM_VAPID_KEY` | vides → push désactivé | push FCM |
| `WEBPUSH_ENABLED`, `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT` | vides → paire générée | Web Push |
| `SEED_ADMIN_*` | désactivé par défaut | bootstrap admin |

## 9. Déploiement serveur (VM OVH)

### 9.1 Modèle de déploiement

- Les images sont **construites par la CI** et publiées sur GHCR (`ghcr.io/<owner>/payment-platform/<service>` + `angular`).
- La VM ne fait que `git fetch`, `docker compose pull` et `up -d` : **aucun build, aucun test, aucun Sonar** sur le serveur.
- Chaque déploiement épingle le tag immuable `sha-<12>` du commit ; `test-latest` / `prod-latest` restent des alias de commodité.
- Exécution des commandes en root via `sudo -i`, puis droits rendus à l'utilisateur SSH. Plafond JVM `-Xmx384m` pour la VM 3,8 Go.
- TLS terminé par le **nginx hôte** (ports 80/443) ; les conteneurs restent en HTTP et ne sont publiés que sur `127.0.0.1`.

### 9.2 Préparation de la VM (une seule fois)

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git nginx certbot
sudo systemctl enable --now docker nginx

sudo mkdir -p /opt/payment-platform
sudo chown "$USER" /opt/payment-platform
cd /opt/payment-platform
git clone <url-du-depot>.git .
cp deploy/.env.example deploy/.env
nano deploy/.env
```

Renseigner au minimum dans `deploy/.env` :
`JWT_SECRET`, `INTERNAL_SECRET`, `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `RABBITMQ_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, `WEBAUTHN_RP_ID`, `WEBAUTHN_ALLOWED_ORIGINS`, `AUTH_COOKIE_SECURE=true`, puis les secrets SMTP/Firebase/VAPID si activés.

Vérifier que `deploy/.env` reste hors Git et que les ports 80/443 sont ouverts (le reste est interne).

### 9.3 Déploiement manuel

```bash
cd /opt/payment-platform
git fetch origin
git checkout --detach <sha-du-commit>
export IMAGE_TAG=sha-<12>
docker compose -f deploy/docker-compose.yml pull
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

Accès aux outils internes (gateway, Adminer, RabbitMQ, Mailpit) via tunnel SSH :

```bash
ssh -L 8081:127.0.0.1:8081 -L 8086:127.0.0.1:8086 -L 15673:127.0.0.1:15673 ubuntu@<vm>
```

### 9.4 CI/CD et secrets GitHub

| Élément | Valeur / rôle |
|---|---|
| `release/**` | build + tests + publication auto vers **TEST** |
| `master` | build + tests + publication auto vers **PROD** (environnement protégé) |
| `develop` et autres branches | build + tests, **aucun déploiement** |
| `v*` (tag) | validation version + images `prod-latest` + GitHub Release |
| `workflow_dispatch` | publication manuelle vers `test` ou `prod` |
| Secrets TEST | `OVH_TEST_HOST`, `OVH_TEST_USER`, `OVH_TEST_SSH_KEY` ; optionnels `OVH_TEST_PORT`, `OVH_TEST_DIR`, `OVH_TEST_URL` |
| Secrets PROD | `OVH_PROD_HOST`, `OVH_PROD_USER`, `OVH_PROD_SSH_KEY` ; optionnels `OVH_PROD_PORT`, `OVH_PROD_DIR`, `OVH_PROD_URL` |
| Environnement `production` | reviewers obligatoires (Settings → Environments) |

Si les secrets sont absents ou si l'hôte se termine par `.invalid`, le déploiement est simulé (run vert) sans impact. Clé SSH de déploiement : sans passphrase, stockée dans le secret (contenu privé complet, LF uniquement).

### 9.5 HTTPS et nginx hôte

Architecture : nginx hôte (systemd) termine TLS et reverse-proxy vers le conteneur `angular` sur `127.0.0.1:8080`. Le conteneur n'expose ni 80 ni 443.

```bash
sudo cp deploy/nginx/host/default /etc/nginx/sites-available/default
sudo cp deploy/nginx/host/ip-https /etc/nginx/sites-available/ip-https
sudo cp deploy/nginx/host/payment.tn-https /etc/nginx/sites-available/payment.tn-https
sudo ln -sf /etc/nginx/sites-available/payment.tn-https /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

- Adapter `server_name` et `ssl_certificate` (`/etc/letsencrypt/live/<identifiant>/`).
- Vhosts disponibles : `default` (80 → 301 HTTPS + webroot ACME), `ip-https` (accès par IP, sans passkeys), `domain-https` (domaine nip.io), `payment.tn-https` (domaine public + sous-domaine PWA).
- Certificat Let's Encrypt :

```bash
sudo certbot certonly --webroot -w /var/www/html \
  -d payment.tn -d www.payment.tn -d nextera.payment.tn
```

- DNS : les enregistrements A de `payment.tn`, `www.payment.tn` et `nextera.payment.tn` doivent pointer vers la VM. Supprimer les AAAA (parking OVH) sous peine d'erreur de certificat en IPv6.
- Passer les origines en HTTPS dans `deploy/.env` puis redéployer :

```dotenv
CORS_ALLOWED_ORIGINS=https://payment.tn,https://www.payment.tn,https://nextera.payment.tn
WEBAUTHN_ALLOWED_ORIGINS=https://payment.tn,https://www.payment.tn,https://nextera.payment.tn
WEBAUTHN_RP_ID=payment.tn
FRONTEND_URL=https://nextera.payment.tn
AUTH_COOKIE_SECURE=true
```

- Vérifications : `curl -I http://<vm>/` → `301` ; `curl -I https://<vm>/` → `200` ; `curl -s https://<vm>/api/auth/me` → JSON `401` (pas le `index.html`).
- Le renouvellement reste géré par `certbot renew` sur l'hôte. Le vhost 80 renvoie les challenges ACME en HTTPS : les deux vhosts doivent servir `/.well-known/acme-challenge/`.

### 9.6 Passkeys et Web Push en production

```dotenv
# Passkeys (identity-service)
WEBAUTHN_RP_ID=payment.tn
WEBAUTHN_RP_NAME=NEXTERA
WEBAUTHN_ALLOWED_ORIGINS=https://payment.tn,https://www.payment.tn,https://nextera.payment.tn
WEBAUTHN_CHALLENGE_TTL_SECONDS=300
WEBAUTHN_TIMEOUT_SECONDS=60

# Web Push (notification-service)
WEBPUSH_ENABLED=true
VAPID_PUBLIC_KEY=<npx web-push generate-vapid-keys>
VAPID_PRIVATE_KEY=<...>
VAPID_SUBJECT=mailto:admin@paymentplatform.tn
```

Points de vigilance :
1. **HTTPS obligatoire** pour WebAuthn et Push (ou `localhost` en dev).
2. `WEBAUTHN_RP_ID` doit être un suffixe de domaine de l'origine ; utiliser l'eTLD+1 (`payment.tn`) pour couvrir les sous-domaines. Une IP ne fonctionne pas.
3. Les clés VAPID doivent rester stables : sinon les abonnements existants deviennent invalides.
4. Le service worker (`ngsw-worker.js`) doit être servi sans cache obsolète après déploiement.
5. iOS : push uniquement pour une PWA ajoutée à l'écran d'accueil ; passkeys via iCloud Keychain. Android : Google Password Manager et FCM.
6. Une session par hôte (cookie `pp_rt` host-only) et une PWA distincte par origine installée.

### 9.7 Backups et restauration (PROD)

- Service `db-backup` (profil compose `backup`, actif en PROD) : dump des 4 bases (`--single-transaction`, routines + events) toutes les 8 h, rotation selon `BACKUP_RETENTION_DAYS` (défaut 7 j), fichiers dans `deploy/backups/` (gitignoré).
- Backup pré-prod automatique avant chaque déploiement PROD (`pre-prod-<timestamp>`).
- Commandes :

```bash
# Sur la VM PROD, depuis la racine du repo
sh deploy/backup/backup.sh db                 # tag db-AAAAMMJJ-HHMMSS
sh deploy/backup/backup.sh release 1.1.0      # tag release-1.1.0
docker compose -f deploy/docker-compose.yml --profile backup exec db-backup /scripts/backup-now.sh [tag]
sh deploy/backup/restore.sh <fichier.sql.gz>  # confirmation RESTORE, écrase les données
```

- Ménage : `docker image prune -a -f`, `docker builder prune -af`, `docker system df`. **Jamais** `docker volume prune` ni `down -v` en PROD.

### 9.8 Rollback et exploitation

- Rollback = redéployer un ancien tag `sha-*` :

```bash
cd /opt/payment-platform
export IMAGE_TAG=sha-<ancien>
docker compose -f deploy/docker-compose.yml pull
docker compose -f deploy/docker-compose.yml up -d
```

- Health check : `curl -sf https://<domaine>/actuator/health` et `curl -sf https://<domaine>/`.
- Logs : `docker compose -f deploy/docker-compose.yml logs -f <service>`.
- Observabilité : logs structurés, correlation-id, healthchecks Spring, consoles RabbitMQ/Adminer/Mailpit en dev.

## 10. Versioning (SemVer)

- Format `MAJEUR.MINEUR.PATCH` (ex. `1.2.0`) : MAJEUR = rupture, MINEUR = fonctionnalité, PATCH = correctif.
- Sources uniques, bumpées **ensemble** à chaque release : `backend/pom.xml` (`<version>`), `payment-platform-ui/package.json` (`"version"`), `sonar.projectVersion` (`sonar-project.properties`).
- Branches :
  - `master` : production, toujours déployable. Chaque merge de release = **tag `vX.Y.Z`**.
  - `develop` : intégration (build + tests, jamais déployée). Après une release, elle porte la version suivante en `-SNAPSHOT` côté backend.
  - `release/X.Y.Z` : coupée depuis `develop` pour stabiliser → **déploiement auto vers TEST** à chaque push → merge vers `master` (+ tag) et back-merge vers `develop`.
  - `hotfix/X.Y.Z` : coupée depuis `master` pour un correctif prod urgent → merge vers `master` (+ tag PATCH) et back-merge vers `develop`.
- Commandes types :
  - `git checkout -b release/1.1.0 develop` (puis bumper les 3 versions à `1.1.0`)
  - `git checkout master && git merge --no-ff release/1.1.0 && git tag v1.1.0 && git push origin master --tags`
  - `git checkout develop && git merge --no-ff release/1.1.0 && git push origin develop`
