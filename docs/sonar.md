# SonarQube / SonarCloud – Guide d'intégration

> Chiffres d'en-tête datés (« 65 tests », « 54 specs », « Angular 21 » — le front est en
> Angular 16, 61 `*.spec.ts`, 19 specs Cypress). Fond du guide valable. Index : [README.md](README.md).

> **Objectif** : qualité continue **code + tests** pour backend (Java 26, Maven, 65 tests) et frontend (Angular 21, 54 specs). Coverage JaCoCo (XML) + lcov + Quality Gate bloquant en CI.

---

## 1. Architecture Sonar choisie

| Brique | Scanner | ProjectKey | Sources | Coverage |
|---|---|---|---|---|
| **Backend** | `org.sonarsource.scanner.maven:sonar-maven-plugin` (Maven) | `payment-platform-backend` | `backend/*/src/main/java` | `backend/**/target/site/jacoco/jacoco.xml` + `target/site/jacoco-aggregate/jacoco.xml` |
| **Frontend** | `sonarsource/sonarqube-scan-action` (CLI) | `payment-platform-frontend` | `payment-platform-ui/src` | `payment-platform-ui/coverage/payment-platform-ui/lcov.info` |
| **Monorepo** (optionnel) | `sonar-scanner` CLI à la racine | `payment-platform` | `backend` + `payment-platform-ui/src` + `frontend/src` | les deux |

**Recommandation** :
- **SonarCloud** (production) : 2 projets séparés (backend / frontend) → Quality Gates indépendants, pricing/organisation plus simple.
- **SonarQube local** (dev) : 1 projet monorepo `payment-platform` via `deploy/docker-compose.sonar.yml` + scan racine.

---

## 2. Backend – Maven + JaCoCo

### 2.1 `backend/pom.xml` (parent)

Propriétés Sonar centralisées (surchargables par `-D`) :

```xml
<sonar.host.url>http://localhost:9000</sonar.host.url>
<sonar.projectKey>payment-platform-backend</sonar.projectKey>
<sonar.coverage.jacoco.xmlReportPaths>
  ${project.basedir}/shared-lib/target/site/jacoco/jacoco.xml,
  ...
  ${project.basedir}/target/site/jacoco-aggregate/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
<sonar.exclusions>...</sonar.exclusions>
<sonar.coverage.exclusions>...</sonar.coverage.exclusions>
```

Plugins activés sur **chaque module** :

- `maven-surefire-plugin` (unit) et `maven-failsafe-plugin` (IT/H2Test)
- `jacoco-maven-plugin` : `prepare-agent` → `report` (phase `test`) → `prepare-agent-integration` → `report-integration` (phase `post-integration-test`)
- `sonar-maven-plugin:5.1.0.4753`

Agrégat au parent : `report-aggregate` (phase `verify`) → `backend/target/site/jacoco-aggregate/jacoco.xml`

### 2.2 Commandes locales

```bash
# 1) Tests + coverage (H2 + JaCoCo XML)
cd backend
./mvnw clean verify

# Vérifier rapports
find . -name "jacoco.xml" -exec ls -lh {} \;
# backend/identity-service/target/site/jacoco/jacoco.xml
# backend/target/site/jacoco-aggregate/jacoco.xml

# 2) Scan SonarQube local (docker-compose.sonar.yml doit tourner)
docker compose -f deploy/docker-compose.sonar.yml up -d
# ou : docker compose -f deploy/docker-compose.dev.yml --profile sonar up -d
# UI : http://localhost:9000 (admin/admin)
# Générer token : My Account → Security → Tokens

# Linux/Mac
export SONAR_TOKEN=squ_xxx
export SONAR_HOST_URL=http://localhost:9000
./mvnw sonar:sonar -Dsonar.host.url=$SONAR_HOST_URL

# Windows PowerShell
$env:SONAR_TOKEN="squ_xxx"
$env:SONAR_HOST_URL="http://localhost:9000"
.\mvnw.cmd sonar:sonar -Dsonar.host.url=$env:SONAR_HOST_URL

# En une ligne : verify + sonar
./mvnw clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000

# SonarCloud
./mvnw verify sonar:sonar \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=<ORG> \
  -Dsonar.token=$SONAR_TOKEN
```

### 2.3 Seuils JaCoCo (optionnel)

Le `check` a été retiré du `pluginManagement` pour ne pas bloquer le build local.  
Pour activer un seuil local (ex: CI sans Sonar) :

```bash
mvn verify -Pcoverage  # profile coverage à étendre si besoin
# ou ajouter une execution <id>check</id> avec <minimum>0.6</minimum>
```

Le **Quality Gate Sonar** reste la porte principale (bloquant en CI).

---

## 3. Frontend – Angular + Karma + lcov

### 3.1 `payment-platform-ui/karma.conf.js` (et `frontend/karma.conf.js`)

```js
coverageReporter: {
  dir: require('path').join(__dirname, './coverage/payment-platform-ui'),
  subdir: '.',
  reporters: [{ type: 'html' }, { type: 'lcovonly' }, { type: 'text-summary' }]
}
browsers: ['Chrome'],
customLaunchers: {
  ChromeHeadlessCI: {
    base: 'ChromeHeadless',
    flags: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
  }
}
```

### 3.2 `angular.json`

```json
"test": {
  "builder": "@angular-devkit/build-angular:karma",
  "options": {
    "karmaConfig": "karma.conf.js",
    ...
  }
}
```

### 3.3 `package.json` scripts

```json
"test:ci": "ng test --code-coverage --watch=false --browsers=ChromeHeadlessCI",
"test:coverage": "ng test --code-coverage --watch=false --browsers=ChromeHeadlessCI"
```

### 3.4 Commandes locales

```bash
cd payment-platform-ui
npm ci --legacy-peer-deps
npm run test:coverage
# OU
npx ng test --code-coverage --watch=false --browsers=ChromeHeadlessCI

ls coverage/payment-platform-ui/
# lcov.info  coverage-final.json  html/

# Scan Sonar local (après avoir lancé SonarQube)
export SONAR_TOKEN=squ_xxx
npx sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.projectKey=payment-platform-frontend

# SonarCloud (utilise payment-platform-ui/sonar-project.properties)
npx sonar-scanner \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=<ORG>
```

---

## 4. Monorepo (racine)

Fichier `sonar-project.properties` à la racine :

```properties
sonar.projectKey=payment-platform
sonar.sources=backend,frontend/src,payment-platform-ui/src
sonar.coverage.jacoco.xmlReportPaths=backend/.../jacoco.xml,backend/target/site/jacoco-aggregate/jacoco.xml
sonar.javascript.lcov.reportPaths=payment-platform-ui/coverage/payment-platform-ui/lcov.info
sonar.qualitygate.wait=true
```

Usage :

```bash
# 1) Build backend + frontend avec coverage
cd backend && ./mvnw clean verify && cd ..
cd payment-platform-ui && npm run test:coverage && cd ..

# 2) Scanner monorepo
sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=$SONAR_TOKEN
```

---

## 5. Docker – SonarQube local

### Option A : profil `sonar` dans `docker-compose.dev.yml`

```bash
docker compose -f deploy/docker-compose.dev.yml --profile sonar up -d
# démarre mysql, rabbitmq, redis + sonarqube + sonar-postgres
docker compose -f deploy/docker-compose.dev.yml --profile sonar ps
open http://localhost:9000
```

### Option B : compose dédié léger

```bash
docker compose -f deploy/docker-compose.sonar.yml up -d
docker logs -f payment-sonarqube
# attendre "SonarQube is operational"
```

Services :

| Container | Image | Port | Volume |
|---|---|---|---|
| `payment-sonarqube` | `sonarqube:community` | `9000:9000` | `sonarqube-data`, `extensions`, `logs` |
| `payment-sonar-postgres` | `postgres:16-alpine` | — | `sonar-postgres-data` |

Stop :

```bash
docker compose -f deploy/docker-compose.sonar.yml down
# ou
docker compose -f deploy/docker-compose.dev.yml --profile sonar down
# pour tout supprimer : down -v
```

---

## 6. CI GitHub Actions (`.github/workflows/ci.yml`)

### Déclenchement

`push` sur `main`/`develop`, `pull_request` sur `main`/`develop`, `workflow_dispatch`.

### Jobs

| Job | Dépendances | Description |
|---|---|---|
| `build-backend` | – | `mvn verify` (H2, Testcontainers) → JaCoCo XML → artifact `jacoco-reports` + `backend-jars` |
| `build-frontend` | – | `npm ci` → `ng test --code-coverage --browsers=ChromeHeadlessCI` → `coverage/lcov.info` → artifact `frontend-coverage` + `frontend-dist` |
| `sonar-backend` | `build-backend` | `mvn verify sonar:sonar` + `sonarqube-quality-gate-action` (poll 5min) – **skip si `SONAR_TOKEN` vide** |
| `sonar-frontend` | `build-frontend` | `npm ci` + `ng test --code-coverage` + `sonarqube-scan-action` (projectBaseDir `payment-platform-ui`) + Quality Gate – **skip si `SONAR_TOKEN` vide** |
| `build-images` | `build-backend`, `build-frontend` | `docker build` 5 microservices + Angular |
| `e2e-tests` | `build-images` | `docker compose up` → Cypress |

### Secrets requis (Settings → Secrets and variables → Actions)

| Secret | Exemple | Obligatoire |
|---|---|---|
| `SONAR_TOKEN` | `squ_xxx` ou `scc_xxx` (SonarCloud) | oui pour activer les jobs Sonar |
| `SONAR_HOST_URL` | `https://sonarcloud.io` ou `https://sonar.entreprise.fr` | non (défaut `https://sonarcloud.io`) |
| `SONAR_ORGANIZATION` | `my-org` | seulement pour SonarCloud |

Sans `SONAR_TOKEN`, les jobs Sonar sont **skipped** (CI verte) – utile pour les forks.

### Quality Gate bloquant

```yaml
- uses: sonarsource/sonarqube-quality-gate-action@v1
  with:
    pollingTimeoutSec: 300
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

Si le Quality Gate est rouge → job échoué → PR bloquée (si branch protection activée).

Configuration recommandée du Quality Gate (SonarCloud UI) :

- Coverage on New Code ≥ 80%
- Duplications on New Code ≤ 3%
- Maintainability / Reliability / Security = A

### Branche protection

GitHub → Settings → Branches → Add rule → `main` → Require status checks → cochez `build-backend`, `build-frontend`, `sonar-backend`, `sonar-frontend`, `e2e-tests`.

---

## 7. Exclusions & règles

**Backend** (`sonar.coverage.exclusions` dans `pom.xml` et `sonar-project.properties`) :

- `domain/model`, `valueobject`, `event`, `exception`, `engine`, `security`, `repository` (objets pur métier non pertinents)
- `config`, `infrastructure/configuration`, `*Application`, `*Config`, `*MapperImpl`

**Frontend** :

- `assets`, `environments`, `*.module.ts`, `main.ts`, `*.spec.ts`

`sonar.cpd.exclusions` (copier-coller) exclut les tests.

---

## 8. Troubleshooting

| Symptôme | Cause | Solution |
|---|---|---|
| `Not authorized. Please check SONAR_TOKEN` | token invalide / expiré | Régénérer dans Sonar → My Account → Security |
| `Project not found` | `sonar.projectKey` n'existe pas | Créer le projet dans Sonar UI ou `sonar.projectKey` typo |
| `No coverage info for file X` | `jacoco.xml` / `lcov.info` non généré | Vérifier `mvn verify` a produit `target/site/jacoco/jacoco.xml` et `ng test --code-coverage` a produit `coverage/*/lcov.info` |
| `Coverage 0%` | mauvais `xmlReportPaths` | Vérifier chemins dans `pom.xml` / `sonar-project.properties` |
| `ChromeHeadless failed` en CI | sandbox Docker | Utiliser `ChromeHeadlessCI` (flags `--no-sandbox`) |
| `quality gate timeout` | SonarQueue lente | Augmenter `pollingTimeoutSec` à 600 |
| SonarQube ne démarre pas (es) | bootstrap checks | `SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true` déjà mis ; augmenter mémoire Docker à 4Go |

Logs CI :

```bash
mvn -X sonar:sonar  # verbose
cat payment-platform-ui/coverage/payment-platform-ui/lcov.info | head -n 20
curl -u admin:admin http://localhost:9000/api/system/status
```

---

## 9. Check-list d'activation

- [ ] Lancer SonarQube local : `docker compose -f deploy/docker-compose.sonar.yml up -d` → http://localhost:9000
- [ ] Créer 2 projets Sonar : `payment-platform-backend` et `payment-platform-frontend` (ou 1 monorepo `payment-platform`)
- [ ] Générer `SONAR_TOKEN` et l'ajouter aux secrets GitHub
- [ ] (SonarCloud) Ajouter `SONAR_ORGANIZATION` + `SONAR_HOST_URL=https://sonarcloud.io`
- [ ] Pousser sur `main` et vérifier les jobs `sonar-backend` / `sonar-frontend` verts + Quality Gate
- [ ] Activer la branch protection sur `main`

---

## 10. Références

- SonarScanner for Maven : https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner-for-maven/
- SonarQube GitHub Action : https://github.com/SonarSource/sonarqube-scan-action
- JaCoCo Maven : https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Karma coverage : https://karma-runner.github.io/latest/config/coverage.html
- Sonar JS/TS LCOV : https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/test-coverage/javascript-typescript-test-coverage/
