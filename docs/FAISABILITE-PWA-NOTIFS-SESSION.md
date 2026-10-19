# Étude de faisabilité — Notifications PWA : ouverture au click + session persistante

> Statut : **ÉTUDE** — aucun développement engagé. Décision attendue §8.
> Date : 19/10/2026 · Cible : `release/1.3.0` · Constat terrain : PWA Android installée (51.222.6.2).

## 1. Contexte et besoins

| # | Besoin | Constat |
|---|--------|---------|
| B1 | **Le click sur une notification doit rouvrir l'app** (PWA installée) sur la bonne page | La notification est bien reçue et affichée (Web Push), mais le click **ne rouvre rien** (ou ouvre un mauvais contexte) |
| B2 | **L'app doit rester connectée après fermeture** : relancée depuis l'icône ou depuis une notif, plus de re-login | Aujourd'hui chaque relance de la WebAPK retombe sur l'écran de connexion |
| B3 | Les notifications doivent continuer d'arriver **app fermée** | **ACQUIS** — Web Push VAPID fonctionne hors session (constaté en exploitation) |

Contrainte : livrer par phases, sans régression sur l'existant (ngsw, SSE, login, passkeys).

## 2. État des lieux (constaté, sourcé)

### 2.1 Chaîne de notification actuelle

```
Backend (RabbitMQ consumers) ──► WebPushSender.sendToUser()
  payload JSON : { "notification": { title, body, icon, badge, tag,
      "data": { "url": "/dashboard/...", "organizationId": "...",
                "onActionClick": { "default": { "operation": "navigateLastFocusedOrOpen", "url": "/dashboard/..." } } } } }
        │  (nl.martijndwars:web-push, VAPID)
        ▼
Navigateur ── push event ──► ngsw-worker.js (SW actif)
  • handlePush : registration.showNotification(title, options)  ← options inclut data ✓
  • notificationclick : handleClick()  (voir 2.2)
```

- Backend : `backend/notification-service/.../infrastructure/push/WebPushSender.java:96-115` (buildPayload)
- Préférence utilisateur : `pushEnabled` consultée avant envoi (`WebPushSender.java:53`)
- Le canal FCM est **désactivé** (`PUSH_ENABLED=false`) : seul Web Push est actif.
- Le SW est `ngsw-worker.js` (Angular 21, `ServiceWorkerModule`) ; **il possède déjà** les handlers `push` et `notificationclick` (`ngsw-worker.js:1590-1591`).

### 2.2 Pourquoi le click n'ouvre pas l'app aujourd'hui

`ngsw-worker.js handleClick` (L1717-1759) exécute **dans cet ordre** :

1. **`notification.close()` immédiatement** (L1719) — si l'ouverture échoue ensuite, la notif est perdue et l'utilisateur ne voit rien se produire ;
2. `operation: navigateLastFocusedOrOpen` → `clients.matchAll({ type: "window" })[0]` (L1797-1799) → **tout onglet/fenêtre existant sur l'origine est prioritaire** :
   - un **onglet Chrome** ouvert sur le site (même en arrière-plan) **absorbe le click** : `navigate()` + `focus()` sur l'onglet, **la PWA installée n'est pas lancée** ;
   - si `navigate()` échoue (client gelé/frozen Android, navigation refusée → promesse `null`), l'exécution continue sur `null.focus()` → **erreur silencieuse, rien ne s'ouvre** ;
3. `clients.openWindow(urlToOpen)` n'est appelé **que si aucun client n'existe** — c'est le seul chemin qui lance correctement la WebAPK installée.

**Cause la plus probable (hiérarchisée)** :
1. Un client fantôme (onglet Chrome ou instance gelée) absorbe le click — cas le plus fréquent quand l'utilisateur utilise Chrome **et** la PWA sur le même téléphone ;
2. `close()` prématuré qui masque tout échec ultérieur.

Le deep-link est pourtant correct : `data.url` est bien porté par la notif (ngsw transfère l'option `data` — `NOTIFICATION_OPTION_NAMES` L1494-1506 inclut `data`) et l'URL est relative au scope `"/"` du manifest.

### 2.3 Session actuelle — pourquoi l'app « ne reste pas connectée »

- **Tout le cycle de vie auth est en `sessionStorage`** :
  - écriture : `login.service.ts:20-22` (login), `:37-42` (refresh), `login.component.ts` (passkey : user + token), `onboarding.service.ts:59` ;
  - lecture directe (hors LoginService) : `jwt.interceptor.ts:36,62` (token, refreshToken), `notification.service.ts:45-48` (token, user — SSE), `stock.service.ts:13`, `stock-optimization.service.ts:13` (user) ;
  - gardes : `core/auth.guard.ts` et `core/role.guard.ts` via `LoginService.isLoggedIn()` (`login.service.ts:67-69`).
- Une **WebAPK Android relancée = contexte de navigation neuf** → `sessionStorage` vide → `AuthGuard` renvoie vers `/login`. Même chose en ouvrant la notif : l'app s'ouvre, mais déconnectée.
- Le refresh silencieux (interceptor HTTP + SSE) **fonctionne déjà** dès qu'un `refreshToken` est présent — il n'est simplement plus là au réveil.

### 2.4 Durée de vie des tokens (backend)

| Token | Durée | Source |
|-------|-------|--------|
| Access token | 30 min | JWT (`exp` ± iat observé dans les logs SSE) |
| Refresh token | **7 jours** (rotation à chaque usage) | `app.auth.refresh-token-ttl-days` — `RefreshTokenService.java:48`, `application.yml` |

Au-delà de 7 jours sans ouverture, un re-login est **forcé côté serveur** quelle que soit la persistance locale.

### 2.5 Réception hors session — ACQUIS (B3)

- L'abonnement push (`PushManager`) est **persisté par le navigateur**, indépendant de la session Angular ;
- Le backend purge les endpoints expirés (404/410) et vérifie `pushEnabled` — aucun développement requis sur ce volet.

## 3. Options étudiées

### 3.1 B1 — Ouvrir l'app au click sur la notification

| Option | Description | Avantages | Inconvénients | Verdict |
|--------|-------------|-----------|---------------|---------|
| **A. `openWindow` côté backend** | Dans `WebPushSender.buildPayload`, changer `operation` de `navigateLastFocusedOrOpen` à **`openWindow`** : ngsw appellera **toujours** `clients.openWindow(urlToOpen)` (`ngsw-worker.js:1726-1728`), sans jamais absorber par un client existant. Sur Android, une URL dans le scope d'une WebAPK installée **relance l'app installée**. | 1 ligne backend + test payload ; **aucun risque frontend** ; effet immédiat | Sur desktop, un nouvel onglet s'ouvre même si une fenêtre existe (léger); certains vieux Android peuvent ouvrir Chrome au lieu de la WebAPK | ✅ **Recommandée — phase 1** |
| **B. SW custom** (`importScripts('ngsw-worker.js')` + handler `notificationclick` prioritaire avec `stopImmediatePropagation()`) | Contrôle total : close() **après** succès, préférence au client exact scope, logs, retry | Résout le dernier kilomètre (devices qui ouvrent Chrome) | **Dette de maintenance** : le SW custom doit suivre les versions ngsw (cassures possibles aux upgrades Angular) ; à valider seulement si A échoue sur le terrain | 🅱️ Réserve (preuve d'échec de A) |
| C. Retirer le handler ngsw | Auto-display + comportement navigateur natif | — | Impossible sans SW custom (ngsw est le SW) ; comportement moins prévisible | ❌ Écartée |

**Note UX associée** : le payload contient déjà le deep-link `data.url` (ex. `/dashboard/payments`). Couplé à la persistance de session (3.2), la notif ouvre l'app **connectée, sur la bonne page** — le parcours notif → action est bouclé en un tap.

### 3.2 B2 — Session persistante

| Option | Description | Avantages | Inconvénients | Verdict |
|--------|-------------|-----------|---------------|---------|
| **A. localStorage pour token / refreshToken / user** | Centraliser l'accès dans `LoginService` (helpers `getToken` / `getRefreshToken` / `getStoredUser` + écriture double ou migration) et basculer les lectures directes (§2.3) sur ces helpers ; purge complète au `logout` | Simple, aligné sur l'architecture actuelle **Bearer + refresh rotation** ; **zéro changement backend** ; effet immédiat : relance WebAPK = toujours connecté (jusqu'au TTL serveur) | `localStorage` lisible en JS (exposition XSS) — déjà partiellement le cas via `sessionStorage` (même origine) ; atténué par access 30 min + rotation serveur | ✅ **Recommandée — phase 1** |
| **B. Refresh token en cookie `HttpOnly`** | Backend `Set-Cookie` sur `/api/auth/refresh` (Secure, HttpOnly, SameSite=Strict) ; le JS ne voit plus le refresh token | Durcissement XSS réel | Touches backend + proxy + gestion CSRF/CORS ; refonte de l'interceptor ; délai plus long | 🅱️ Phase 2 (évolution) |

**Durée de vie** : conserver le défaut 7 jours, mais **exposer `REFRESH_TOKEN_TTL_DAYS` dans `.env` de déploiement** pour ajuster **sans rebuild**. Recommandation métier mobile-terrain : **30 jours** (rotation active conservée ; au-delà, re-login forcé).

**Multi-appareils** : chaque téléphone possède sa session et son abonnement push propres et indépendants — « rester connecté » vaut **par appareil** (limite structurelle, pas un défaut).

## 4. Phase 1 proposée (périmètre)

1. **Backend** — `WebPushSender.buildPayload` : `operation` → `"openWindow"` ; test unitaire du payload (opération + url + data).
2. **Frontend** — `LoginService` : bascule `localStorage` (`token`, `refreshToken`, `user`) avec migration au boot (si valeurs sessionStorage et localStorage vides → copie puis purge sessionStorage) ; helpers d'accès centralisés ; `logout()` purge les deux stockages.
3. **Frontend** — migration des lectures directes sur les helpers : `jwt.interceptor.ts`, `notification.service.ts`, `stock.service.ts`, `stock-optimization.service.ts`, `onboarding.service.ts`.
4. **Déploiement** — `REFRESH_TOKEN_TTL_DAYS` (30) dans `deploy/.env` + `application.yml` (property déjà lue par Spring).
5. **Tests** — unitaires Karma (LoginService storage/migration/logout, interceptor avec helpers) ; backend (payload) ; **recette manuelle PWA Android** décrite §7.

Hors phase 1 : SW custom (option B §3.1), cookie httpOnly (option B §3.2), iOS.

## 5. Limites connues

- **iOS Safari / PWA** : web push iOS 16.4+ exige l'ajout à l'écran d'accueil et un comportement `notificationclick` différent ; non traité en phase 1 (cible Chrome Android).
- **Android 13+** : la permission système « Notifications » de l'app installée est indépendante de celle du navigateur — si l'utilisateur l'a refusée pour l'app, rien ne s'affiche (déjà correct chez l'utilisateur test).
- **TTL serveur** : au-delà du `REFRESH_TOKEN_TTL_DAYS`, la session est invalidée côté serveur même si le localStorage est intact → écran de login (comportement voulu).

## 6. Risques et atténuations

| Risque | Impact | Atténuation |
|--------|--------|-------------|
| `localStorage` lisible par script (XSS) | Vol possible de refresh token | Access token court (30 min), rotation serveur à chaque refresh, pas de secret longue durée côté JS ; phase 2 : cookie HttpOnly |
| `openWindow` ouvre un onglet Chrome au lieu de la WebAPK sur certains appareils | UX dégradée au click | Mesure terrain phase 1 → déclenchement de l'option B (SW custom) si avéré |
| Session « collante » sur téléphone partagé | Confidentiailité | Logout explicite déjà présent ; évolutions possibles : « déconnexion des autres appareils » (tableau subscriptions push déjà existant) |
| SW custom (option B) si activée | Dette ngsw | Réservée à preuve d'échec de A |

## 7. Critères d'acceptation (recette manuelle — PWA Android)

1. App **fermée** (swiped), notif reçue → **click → la PWA installée se rouvre** directement sur l'URL du deep-link ;
2. App fermée puis relancée depuis l'icône : **connecté** sans login, jusqu'à 30 jours d'inactivité ;
3. Après expiration serveur (> TTL) : retour propre sur `/login` ;
4. `logout()` : purges localStorage + sessionStorage, redirection login, plus de refresh ;
5. Régression zéro : SSE/badge notifs, login passkey, logout, pages admin/shop/supplier.

## 8. Décision demandée

- [ ] **D1** — Valider l'**option A** (`openWindow`) pour l'ouverture au click (phase 1), avec l'option B (SW custom) en réserve documentée.
- [ ] **D2** — Valider la **session localStorage** + exposition `REFRESH_TOKEN_TTL_DAYS` (recommandé : 30 j) en déploiement.
- [ ] **D3** — Confirmer ou écarter la **phase 2** (refresh token en cookie HttpOnly).

Estimation phase 1 : **≈ 2,5 j** (payload 0,5 · frontend session 1 · config+tests 0,5 · recette 0,5).
