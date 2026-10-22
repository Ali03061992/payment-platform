# Passkeys (WebAuthn) & Notifications Web Push

Ce document couvre l'authentification sans mot de passe (passkeys) et les notifications
push de la PWA : architecture, configuration, déploiement et spécificités iOS/Android.

---

## 1. Passkeys / WebAuthn

### 1.1 Architecture

| Couche | Élément |
|---|---|
| Stockage | `webauthn_credentials` (V9), `webauthn_challenges` (V10) — **identity-service** |
| Métier | `WebAuthnRegistrationUseCase`, `WebAuthnLoginUseCase`, `WebAuthnCredentialManagementUseCase` |
| API | `WebAuthnController` — `/api/auth/webauthn/**` (proxifié par api-gateway) |
| Sécurité | `/api/auth/webauthn/login` et `/login/options` **publics** (rate-limités `RATE_LIMIT_AUTH_PER_MINUTE` au gateway) ; enregistrement et gestion **JWT requis** |
| Librairie | `org.springframework.security:spring-security-webauthn:7.1.0` (désérialisation via `WebauthnJacksonModule`), validation **webauthn4j** |
| Frontend | `WebAuthnService` (`web-authn.service.ts`), page **Sécurité**, bouton « Passkey » sur la page de login |

Endpoints :

| Méthode | Route | Auth | Rôle |
|---|---|---|---|
| POST | `/api/auth/webauthn/register/options` | JWT | Démarrer l'enregistrement |
| POST | `/api/auth/webauthn/register` | JWT | Confirmer (payload `navigator.credentials.create`) |
| GET | `/api/auth/webauthn/credentials` | JWT | Lister les appareils |
| PATCH | `/api/auth/webauthn/credentials/{id}` | JWT | Renommer |
| DELETE | `/api/auth/webauthn/credentials/{id}` | JWT | Révoquer |
| POST | `/api/auth/webauthn/login/options` | Public | Options de connexion (username-first ou discoverable) |
| POST | `/api/auth/webauthn/login` | Public | Assertion → tokens JWT/refresh |

Le défi est stocké côté serveur (challenge TTL par défaut 300 s), lié à l'utilisateur et
à la finalité (REGISTER/LOGIN) ; il est rejoué puis supprimé après usage (anti-rejeu).

### 1.2 Variables d'environnement (identity-service)

| Variable | Profil | Défaut | Description |
|---|---|---|---|
| `WEBAUTHN_RP_ID` | tous / **prod requis** | `localhost` | **RP ID** = domaine hébergeant la PWA, **sans schéma ni port** (ex. `pay.example.tn`). Doit être un suffixe de domaine de l'origine (eTLD+1 recommandé). |
| `WEBAUTHN_RP_NAME` | tous | `NEXTERA` | Nom affiché dans le dialogue du navigateur. |
| `WEBAUTHN_ALLOWED_ORIGINS` | tous / **prod requis** | `http://localhost:4200,http://localhost:8080` | Origines autorisées (RFC 6454), séparées par des virgules, **avec schéma**. |
| `WEBAUTHN_CHALLENGE_TTL_SECONDS` | tous | `300` | Durée de validité d'un défi. |
| `WEBAUTHN_TIMEOUT_SECONDS` | tous | `60` | Timeout renvoyé aux options. |

> En **prod**, `WEBAUTHN_RP_ID` et `WEBAUTHN_ALLOWED_ORIGINS` sont **obligatoires** (pas de
> défaut dans `application-prod.yml`) : le service refuse de démarrer sans eux.

> Les challenges sont **à usage unique** : consommation atomique en base et purge des challenges expirés avant chaque émission.

### 1.3 Frontend

- **Login** : bouton « Se connecter avec une passkey » (affiché si `window.PublicKeyCredential`
  existe). Si un nom d'utilisateur est saisi, flux *username-first* (`allowCredentials` renseigné) ;
  sinon flux *discoverable* (identifiant Resident Key / user handle).
- **Page Sécurité** (`/dashboard/security`, nav `NAV.SECURITY`, tous rôles) :
  liste des appareils (label, dernière utilisation, transports, statut), ajout avec label,
  renommage, révocation (confirmation), et section notifications push.
- Conversion base64url ↔ `ArrayBuffer` gérée dans `WebAuthnService` ; le payload republié
  utilise le format sérialisé par `WebauthnJacksonModule` (`id`, `rawId`, `type: "public-key"`,
  `response.clientDataJSON`, `response.attestationObject` / `authenticatorData` + `signature`
  + `userHandle`, `clientExtensionResults`).
- Aucune permission ni prompts automatiques au login : tout est opt-in explicite (page Sécurité).

---

## 2. Notifications Web Push (PWA)

### 2.1 Architecture

| Couche | Élément |
|---|---|
| Stockage | `web_push_subscriptions` (V4), `notification_preferences` (V5), `vapid_keypairs` (V6) — **notification-service** |
| Métier | `WebPushSubscriptionService`, `NotificationPreferenceService` |
| Envoi | `WebPushSender` (`@Async`, lib `nl.martijndwars:web-push` + jose4j + BouncyCastle), payloads conformes au contrat **ngsw-worker.js** |
| Clés | `VapidKeyProvider` : property-first, sinon génération EC P-256 persistée en base |
| API | `WebPushController` — `/api/push/**` (route gateway `/push/**` déjà ajoutée) |
| Frontend | `WebPushService` (`SwPush` + `ServiceWorkerModule` déjà actif), opt-in depuis la page Sécurité |

Endpoints :

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/push/vapid-public-key` | Clé publique VAPID (base64url) pour `PushManager.subscribe` |
| POST | `/api/push/subscriptions` | Enregistrer un abonnement `{endpoint, p256dh, auth, userAgent}` |
| GET | `/api/push/subscriptions` | Lister les abonnements de l'utilisateur |
| DELETE | `/api/push/subscriptions?endpoint=...` | Se désabonner |
| DELETE | `/api/push/subscriptions/{id}` | Se désabonner (par id) |
| GET/PUT | `/api/push/preferences` | Préférences `{pushEnabled, emailEnabled}` |

Les événements métier (paiement, commande, litige) publiés via RabbitMQ déclenchent déjà
l'envoi push en plus du reste de la chaîne de notification. Les endpoints expirés (404/410)
sont purgés automatiquement à l'envoi.

### 2.2 Variables d'environnement (notification-service)

| Variable | Défaut | Description |
|---|---|---|
| `WEBPUSH_ENABLED` | `true` | Coupe l'envoi push (le reste des notifications continue). |
| `VAPID_PUBLIC_KEY` | *(vide)* | Clé publique VAPID (ECDSA P-256, base64url). |
| `VAPID_PRIVATE_KEY` | *(vide)* | Clé privée VAPID. |
| `VAPID_SUBJECT` | `mailto:admin@paymentplatform.tn` | `mailto:` ou `https:` contacté par les push services. |

Génération d'une paire :

```bash
npx web-push generate-vapid-keys
```

- **Vides** → une paire est générée au premier démarrage et persistée dans `vapid_keypairs`
  (convenable en dev).
- **Prod** → renseigner `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` dans `.env` : les clés doivent
  rester stables, sous peine que les abonnements existants deviennent invalides.

### 2.3 Frontend / Service worker

- Abonnement via `SwPush.requestSubscription({ serverPublicKey })` puis enregistrement serveur ;
  désabonnement par `DELETE /api/push/subscriptions?endpoint=...`.
- Au logout, le front désabonne le `PushSubscription` navigateur, supprime les tokens FCM/Web Push côté serveur et purge le Cache Storage (données utilisateur hors ligne).
- Le service worker Angular (`ngsw-worker.js`) affiche la notification à partir du champ
  `notification` du payload (`title`, `body`, `icon`, `badge`, `tag`,
  `data.onActionClick.default` pour la navigation au clic).
- `ServiceWorkerModule` n'est actif qu'en build de production : le push nécessite donc un build
  `ng build --configuration production` servi en **HTTPS**.
- Le canal FCM existant (`PushNotificationService`) coexiste ; la permission n'est plus
  demandée automatiquement au login (opt-in explicite uniquement).

---

## 3. Déploiement

`.env` (voir `deploy/.env.example`) :

```dotenv
# Passkeys
WEBAUTHN_RP_ID=pay.example.tn
WEBAUTHN_RP_NAME=NEXTERA
WEBAUTHN_ALLOWED_ORIGINS=https://pay.example.tn

# Web Push
WEBPUSH_ENABLED=true
VAPID_PUBLIC_KEY=<npx web-push generate-vapid-keys>
VAPID_PRIVATE_KEY=<...>
VAPID_SUBJECT=mailto:admin@paymentplatform.tn
```

`deploy/docker-compose.yml` et `deploy/docker-compose.staging.yml` passent déjà ces variables
aux services `identity-service` et `notification-service`.

Points de vigilance :

1. **HTTPS obligatoire** — WebAuthn (`navigator.credentials`) et Push API ne fonctionnent
   qu'en contexte sécurisé (HTTPS ou `localhost`).
2. **RP ID ↔ origine** — le RP ID doit être un suffixe de domaine de l'origine ; utiliser un
   eTLD+1 (ex. `example.tn`) si la PWA est servie sur plusieurs sous-domaines.
3. **`ngsw-worker.js`** — servi depuis le domaine de la PWA ; toute reverse proxy (voir
   `deploy/nginx/`) doit le laisser accessible sans cache stale après un déploiement
   (les mises à jour passent par le cycle `SwUpdate`).

---

## 4. Spécificités iOS

### Passkeys (Safari, iOS 16+ / macOS 13+)

- Enregistrement/usage via **iCloud Keychain** : les passkeys se synchronisent entre appareils
  Apple (iPhone, iPad, Mac) authentifiés par le même Apple ID.
- Sur iOS, Safari bascule sur l'interface SystemPassword / `authenticationServices` ; le prompt
  natif remplace le dialogue web — pas d'impact sur le payload.
- **Cross-device (continuity)** : une passkey créée sur Mac peut être utilisée sur iPhone via
  QR/Bluetooth — nécessite `allowCredentials` vide ou transport `hybrid`, déjà géré côté serveur
  (les transports sont stockés mais jamais filtrés côté backend).
- Si le RP ID est un sous-domaine, préférer l'eTLD+1 pour éviter les *Related Origin Requests*
  (iOS 17+ émet une requête de vérification d'origine).

### Web Push (Safari, iOS 16.4+)

- **La notification push sur iOS nécessite que la PWA soit ajoutée à l'écran d'accueil**
  (Menu Partager → « Sur l'écran d'accueil ») : Safari mobile ne supporte pas le push pour les
  onglets classiques.
- Une fois installée, la permission se demande **uniquement après une action utilisateur**
  (notre bouton « Activer » de la page Sécurité respecte cette règle).
- Safari émet des subscription endpoints propres à Apple ; ils expirent parfois — la purge
  automatique 404/410 de `WebPushSender` couvre ce cas.
- macOS Safari 16.4+ supporte le push pour les sites classiques (pas seulement les web clips).

---

## 5. Spécificités Android

### Passkeys (Chrome 108+ / Android 8+)

- Gestionnaire de mots de passe Google (**Google Password Manager**) : les passkeys sont
  stockées sur l'appareil Android, liées au compte Google, et proposées par le prompt System
  (Dialogue d'inscription SystemPassword).
- Sur Android, le transport remonté est généralement `internal` ; l'usage **hybrid** (QR code)
  est proposé quand aucun authenticator local n'est disponible — le backend accepte les
  transports `usb`, `nfc`, `ble`, `internal`, `hybrid`, `smart-card`.
- Applications tierces (Samsung Pass, 1Password…) apparaissent dans le même prompt si
  elles enregistrent un *Credential Provider*.

### Web Push (Chrome for Android)

- Supporté nativement (FCM sous le cap), pas de contrainte « installée » contrairement à iOS.
- Le prompt de permission du navigateur ne peut apparaître qu'après une action utilisateur.
- Les endpoints FCM expirent quand l'app est retirée ; la purge 404/410 couvre ce cas.

---

## 6. Limites connues

- Un même label d'appareil peut être utilisé plusieurs fois (pas d'unicité forcée côté serveur).
- La révocation est **locale au compte** : elle empêche l'usage de la credential via la plateforme,
  elle ne révoque pas la paire sur l'authenticator (le prochain login n'offrira plus ce choix).
- Web Push requiert un build de production (ngsw actif) ; en dev (`ng serve`) `SwPush` remonte
  une erreur `Service worker not registered` — la page Sécurité affiche le message dédié.
- FCM et Web Push sont deux canaux indépendants : un appareil peut être abonné aux deux, il
  recevra alors deux notifications. Préférer un seul canal par appareil.
