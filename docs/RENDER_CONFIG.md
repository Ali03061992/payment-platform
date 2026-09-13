# Render Config - payment-platform (srv-dag73th42hec73eh8qkg)

Service actuellement déployé : **https://payment-platform-gsnx.onrender.com**
Commit affiché : `ac72e61` | Status : `Failed` | Branch `main` Ali03061992/payment-platform

---

## 1. Service actuel `srv-dag73th42hec73eh8qkg` - General

| Champ Render Dashboard | Valeur actuelle | Valeur corrigée | Fichier |
|---|---|---|---|
| **Name** | `payment-platform` | garder | `render.yaml:3` |
| **Region** | `Virginia (US East)` | garder | - |
| **Branch** | `main` | garder | `render.yaml` |
| **Root Directory** | *(vide)* | *(vide)* - laisser vide (repo root) | `deploy/docker-compose.yml:66` context `../backend` |
| **Dockerfile Path** | `./backend/Dockerfile` | **garder** ` ./backend/Dockerfile` **OU** ` ./Dockerfile.backend` (voir 1.1) | `backend/Dockerfile:1` `Dockerfile.backend:1` |
| **Docker Build Context Directory** | `.` | **`./backend`** → c'est le bug `lstat /opt/render/project/src/backend : no such file` / `COPY ... not found` ligne 9 | `backend/Dockerfile:3-9` attend `context=backend` |
| **Build Arg** | *(vide)* | **`SERVICE_NAME=api-gateway`** (ou `identity-service` / `organization-service` / `payment-service` / `notification-service` selon le rôle de ce service - actuellement c'est l'API Gateway qui expose `https://payment-platform-gsnx.onrender.com`) | `backend/Dockerfile:15` `ARG SERVICE_NAME` |

### 1.1 Pourquoi le build échoue

```
#1 [internal] load build definition from Dockerfile
error: failed to solve: ... "/notification-service/pom.xml": not found  #9 backend/Dockerfile:9
```
`backend/Dockerfile:3-9` fait :
```dockerfile
COPY pom.xml pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
```
Ces chemins sont relatifs au **contexte**. Avec `Context=.` (racine), Docker cherche `notification-service/pom.xml` à la racine → n'existe pas (il est à `backend/notification-service/pom.xml`).

Avec `Context=./backend`, Docker cherche `notification-service/pom.xml` dans `backend/` → trouvé `backend/notification-service/pom.xml`.

L'erreur suivante `lstat /opt/render/project/src/backend : no such file` vient de `Context=backend` avec `Root Directory` déjà à `backend` → Render cherche `backend/backend`.

**Fix à appliquer dans https://dashboard.render.com/web/srv-dag73th42hec73eh8qkg/settings :**
- `Root Directory` → laisser **vide**
- `Dockerfile Path` → `./backend/Dockerfile`
- `Docker Build Context Directory` → `./backend`
- `Docker Build Arg` → `SERVICE_NAME=api-gateway` (ou le service que tu veux exposer sur `payment-platform-gsnx.onrender.com`)

Alternative si tu veux garder `Context=.` :
- `Dockerfile Path` → `./Dockerfile.backend` (nouveau `Dockerfile.backend:1` qui fait `COPY backend/pom.xml ...` pour contexte racine)

### 1.2 Env Vars à mettre dans Render Dashboard > Environment

```
JWT_SECRET=<generate-a-random-secret-at-least-32-bytes>
JWT_EXPIRATION=30m
SERVICE_NAME=api-gateway
SPRING_PROFILES_ACTIVE=prod
# si service = identity/organization/payment/notification : ajouter MYSQL_* , RABBITMQ_* , REDIS_* , ELASTICSEARCH_*
# voir deploy/.env.example:6-18 et deploy/docker-compose.yml:66-158
```

Pour `api-gateway` seul, ajoute aussi :
```
IDENTITY_SERVICE_URL=payment-platform-identity.onrender.com (private)
IDENTITY_SERVICE_PORT=8082
ORGANIZATION_SERVICE_URL=...
PAYMENT_SERVICE_URL=...
NOTIFICATION_SERVICE_URL=...
CORS_ALLOWED_ORIGINS=https://payment-platform-gsnx.onrender.com,http://localhost:8080
```

### 1.3 Health Check & Deploy

- `Health Check Path` : `/actuator/health` (Spring Boot 4.1.0 `backend/pom.xml:9`)
- `Docker Command` : laisser vide (utilise `ENTRYPOINT ["java", "-jar", "app.jar"]` de `backend/Dockerfile:23`)
- `Auto-Deploy` : `On Commit` (comme actuel)
- `Plan` : Starter (Virginia)

Après `Save Changes` → `Manual Deploy` → `Deploy latest commit` `ac72e61` (ou `9f9497f`).

---

## 2. Déploiement complet (6 services) via Blueprint `render.yaml`

Au lieu d'un seul `payment-platform`, déploie les 6 services comme `deploy/docker-compose.yml:66` :

`render.yaml:1` déjà poussé :

```yaml
services:
  - type: web
    name: payment-platform-gateway      # api-gateway
    env: docker
    dockerfilePath: ./backend/Dockerfile
    dockerContext: ./backend
    envVars: [SERVICE_NAME=api-gateway, JWT_SECRET, ...]
  - type: web
    name: payment-platform-identity
    dockerfilePath: ./backend/Dockerfile
    dockerContext: ./backend
    envVars: [SERVICE_NAME=identity-service]
  - type: web
    name: payment-platform-organization
    ...
  - type: web
    name: payment-platform-payment
    ...
  - type: web
    name: payment-platform-notification
    ...
  - type: web
    name: payment-platform-frontend
    env: docker
    dockerfilePath: ./frontend/Dockerfile
    dockerContext: .
```

Pour l'activer : Render Dashboard → `Blueprints` → `New Blueprint Instance` → sélectionner `Ali03061992/payment-platform` → `render.yaml` → `Apply`. Render va créer les 6 services avec les bons `dockerfilePath`/`dockerContext`/`SERVICE_NAME`.

---

## 3. Vérification locale (même build que Render)

```powershell
# même que Render fait
docker build -f backend/Dockerfile --build-arg SERVICE_NAME=api-gateway -t test ./backend
docker build -f frontend/Dockerfile -t test-frontend .
# ou
docker compose -f deploy/docker-compose.yml build --no-cache api-gateway
docker compose -f deploy/docker-compose.yml up -d --wait
curl http://localhost:8081/actuator/health # 200
curl http://localhost:8080 # 200 frontend
```

Logs Render après fix : `BUILD SUCCESS` `Reactor Summary Payment Platform ... SUCCESS` comme en local `backend: 01:26 min`.

---

## 4. Checklist `srv-dag73th42hec73eh8qkg`

- [ ] `Root Directory` vide
- [ ] `Dockerfile Path` = `./backend/Dockerfile` (ou `./Dockerfile.backend` si `Context=.`)
- [ ] `Docker Build Context` = `./backend` (si `backend/Dockerfile`) ou `.` (si `Dockerfile.backend`)
- [ ] `SERVICE_NAME` renseigné
- [ ] `JWT_SECRET` etc. renseignés
- [ ] `Health Check Path` = `/actuator/health`
- [ ] `Manual Deploy` dernier commit `ac72e61` → `Status: Live`
