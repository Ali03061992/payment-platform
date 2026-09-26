# Rebuild, tests et Sonar en local

Guide opératoire local (Windows PowerShell) : recompiler, tester (unitaires,
Karma, Cypress) et scanner avec SonarQube local. Détails de fond :
[sonar.md](sonar.md), [testing.md](testing.md), `deploy/LOCAL_DEV.md`,
`payment-platform-ui/CYPRESS_E2E_GUIDE.md`.

## 0. Prérequis

| Outil | Version constatée | Vérification |
|---|---|---|
| JDK | 26 | `java -version` |
| Maven wrapper | `backend/mvnw.cmd` (offline possible : `-o`) | — |
| Node.js | 22 + npm 10 | `node --version` |
| Docker Desktop | — | `docker ps` |
| Chrome | — | requis par Karma headless et Cypress |

Dépôt local Maven (`~/.m2`) déjà alimenté : préférer `-o` (offline) pour les
builds répétés, sauf ajout de dépendance.

## 1. Rebuild backend

```powershell
cd backend
# Tout compiler (rapide, sans tests)
.\mvnw.cmd -o -q -pl payment-service,identity-service,organization-service,api-gateway -am compile -DskipTests
```

> **Piège n°1 — shared-lib périmée** : les services résolvent `shared-lib`
> depuis le repo local. Après toute modif de `shared-lib`, passer par le
> réacteur (`-am`) ou réinstaller :
> `.\mvnw.cmd -o -pl shared-lib install -DskipTests -q`.
> Symptôme : `cannot find symbol` / `NoClassDefFoundError` sur une classe
> qui existe pourtant en source.

```powershell
# Rebuild complet + tous les tests (long : ~15-25 min)
.\mvnw.cmd -o clean verify
```

## 2. Tests backend ciblés (boucle rapide)

Toujours avec `-am` (cf. piège n°1) :

```powershell
cd backend
.\mvnw.cmd -o -pl <module> -am test "-Dtest=MaClasseTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false"
# Exemples réellement utilisés :
#   -pl payment-service -am test "-Dtest=PaymentUseCaseH2Test,PaymentControllerTest" ...
#   -pl identity-service -am test "-Dtest=RefreshTokenServiceH2Test,AuthControllerTest" ...
#   -pl organization-service -am test "-Dtest=CatalogControllerTest,OrderControllerTest" ...
#   -pl api-gateway -am test "-Dtest=GatewaySecurityTest,RateLimitFilterTest" ...
```

Conventions : `@SpringBootTest` + `@ActiveProfiles("test")` (H2), MockMvc via
`MockMvcBuilders.webAppContextSetup(wac)` (+ `.apply(springSecurity())` quand
la chaîne de sécurité doit tourner — sinon les requêtes partent en vrai vers
l'aval). Ne jamais mettre `spring.profiles.active` dans un
`application-test.yml` (Boot 4 refuse : `InvalidConfigDataPropertyException`).

## 3. Frontend : tsc + Karma

```powershell
cd payment-platform-ui
npx tsc --noEmit -p tsconfig.app.json          # 0 erreur attendue
npx ng test --watch=false --browsers=ChromeHeadlessCI --include=src/app/services/payment.service.spec.ts
npm run test:coverage                            # suite complète + coverage/payment-platform-ui/lcov.info
```

## 4. Cypress E2E en local

Prérequis STRICTS (sinon cascade de fails) :

1. Infra : MySQL/Redis/RabbitMQ/Mailpit UP (`docker ps`).
2. Les 5 services Java lancés en profil `local` (configs IntelliJ `.run/`,
   ports 8081-8085). Vérifier : `GET http://localhost:8081/actuator/health` → 200.
3. Front servi : `npx ng serve --port 4200` (attendre le `200` sur `/`).
4. Budget rate-limit : le profil `local` du gateway applique
   `rate-limit-auth-per-minute: 1000` (`application-local.yml`) — la suite fait
   ~70 logins depuis une seule IP ; avec le défaut strict (10/min), tout tombe
   en 429. La prod garde 10/min.

```powershell
cd payment-platform-ui
npx cypress run --browser chrome --headless                       # 19 specs, ~8 min
npx cypress run --browser chrome --headless --spec cypress/e2e/06-supplier-stock.cy.ts
```

Référence : 19 specs / 244 tests verts. En cas de 429 massifs, suspect n°1 =
gateway redémarré sans le budget 1000 (vérifier le header
`X-RateLimit-Limit` sur un login en échec).

## 5. SonarQube local

```powershell
# 1) Démarrer SonarQube (http://localhost:9000, admin/admin)
.\deploy\start-sonar.ps1

# 2) Créer un token : http://localhost:9000/account/security  (ex. squ_xxx)

# 3) Scan monorepo (rebuild backend + coverage front + scan dockerisé)
$env:SONAR_TOKEN="squ_xxx"
.\deploy\scan-monorepo.ps1
# UI : http://localhost:9000/dashboard?id=payment-platform

# Re-scan sans rebuild / scans partiels :
.\deploy\scan-monorepo.ps1 -SkipBuild
cd backend; .\mvnw.cmd sonar:sonar -Dsonar.host.url=http://localhost:9000
cd payment-platform-ui; npx sonar-scanner -Dsonar.host.url=http://localhost:9000

# Arrêt :
docker compose -f deploy/docker-compose.sonar.yml down
```

Pré-générés par le scan : `backend/**/target/site/jacoco/jacoco.xml`
(+ agrégat) et `payment-platform-ui/coverage/payment-platform-ui/lcov.info`.
Clés : `payment-platform-backend`, `payment-platform-frontend`, `payment-platform`
(monorepo). En CI les jobs Sonar sont skippés sans secret `SONAR_TOKEN`.

## 6. Dépannage express

| Symptôme | Cause probable | Fix |
|---|---|---|
| `cannot find symbol` / `NoClassDefFoundError` après modif shared-lib | jar local périmé | build avec `-am` ou `install` shared-lib (§1) |
| `Port 808x already in use` (mvnw) | ancien process Java vivant | `Get-NetTCPConnection -LocalPort 808x` → `Stop-Process`, vérifier port libéré |
| Cypress : 429 massifs sur `/api/auth/login` | budget auth strict (10/min) | vérifier `X-RateLimit-Limit: 1000` ; relancer gateway avec la valeur locale |
| Cypress : `cy.visit('/')` timeout partout | front 4200 down | `npx ng serve --port 4200`, attendre 200 |
| MockMvc : la requête part « en vrai » vers l'aval | chaîne Spring Security non appliquée | `.apply(SecurityMockMvcConfigurers.springSecurity())` |
| `expected array, got {items,...}` en E2E | contrat paginé B5 | assertions sur `body.items` + `totalElements` |
| Tests verts en isolé, rouges en suite | état partagé (DB E2E persistante, budget rate-limit) | ordre des specs, `ensureTestUsers`, éviter le hammering |
