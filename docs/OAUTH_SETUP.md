# OAuth / passwordless — setup & mode dev

> Branche : `feature/oauth-passwordless`. FR, factuel, **sans secrets**
> (client IDs publics uniquement, jamais de client secrets dans le repo).

## 1. Pourquoi (plus de password)

Le login password reste le flux par défaut et inchangé. OAuth ajoute une
alternative sans password :

- **Test/prod** : boutons Google / Microsoft sur `/login` → le front
  récupère l'ID token OIDC (GIS/MSAL, aucun secret côté front) puis
  `POST /api/auth/oauth {provider, idToken}`. Le backend vérifie la
  signature/iss/aud/exp via JWKS et résout le compte **par email vérifié**.
- **Local/dev** : `POST /api/auth/dev-login {username}` (sans password,
  actif seulement si flag + profil non-prod) + panneau dev sur `/login`
  pour tester sans IdP. Les specs Cypress 01–19 continuent en password ;
  la spec 20 couvre le mode dev (skippée hors dev).

## 1bis. Règle d'autorisation : l'email doit pré-exister

En test comme en prod, l'OAuth **ne crée aucun compte** :

- l'email (vérifié par Google/Microsoft) doit déjà exister dans la liste
  des utilisateurs, sinon `401 "Aucun compte associé à cet email"` ;
- la session hérite **rôle + organisation de ce compte existant**
  (rôle/partition vers l'organisation depuis le compte, jamais depuis le token) ;
- compte existant mais DISABLED → `403` (workflow de validation M5 inchangé) ;
- la liaison `provider + subject` est mémorisée ; un subject différent déjà
  lié au même provider → `401` (anti-usurpation).

Référence : `system.admin` ↔ `ali.ben.amor.1992@hotmail.com` (compte Microsoft) —
voir `V8__update_system_admin_email.sql`. Les comptes se créent côté admin
(`POST /api/users` + `PATCH /api/users/{id}/activate`), jamais via OAuth.

## 2. Pièces à configurer (registrations console — faites par l'humain)

> NOTE EXPLICITE : les registrations ci-dessous se font **par l'humain**
> dans les consoles cloud (aucun accès API, aucune action auto possible
> depuis le repo). **Sans elles, les boutons restent masqués et
> `POST /api/auth/oauth` répond 503** (IDs absents côté backend).

### 2.1 Google Cloud — OAuth client ID

- Console : <https://console.cloud.google.com> > **APIs & Services >
  Credentials** > **Create Credentials > OAuth client ID** (type
  *Web application*).
- **Authorized JavaScript origins** : origine exacte du front
  (dev `http://localhost:4200`, URL prod). Pas de redirect URI nécessaire
  (flux ID token direct, sans code) — le backend vérifie `aud` = Client ID.
- Récupérer le **Client ID** (public, pas de secret) → `GOOGLE_CLIENT_ID`.

### 2.2 Microsoft Entra — App registration

- Portail : <https://portal.azure.com> > **Microsoft Entra ID >
  App registrations > New registration** (comptes personnels autorisés
  pour les adresses Hotmail/Outlook).
- Redirect URI : plateforme *SPA*, origine du front (requise par MSAL).
- Récupérer **Application (client) ID** → `MICROSOFT_CLIENT_ID` et
  **Directory (tenant) ID** → `MICROSOFT_TENANT_ID`.

## 3. Variables (table complète)

| Variable | Où | Défaut | Effet |
|---|---|---|---|
| `GOOGLE_CLIENT_ID` | `deploy/.env.example`, backend | vide | vide = bouton Google masqué, `/oauth` 503 pour Google |
| `MICROSOFT_CLIENT_ID` | `deploy/.env.example`, backend | vide | vide = bouton Microsoft masqué, `/oauth` 503 pour Microsoft |
| `MICROSOFT_TENANT_ID` | `deploy/.env.example`, backend | vide | tenant Entra utilisé pour valider les tokens Microsoft |
| `IS_DEV` | `deploy/.env.example` → `assets/env.js` via `env.template.js` (envsubst) | vide (= prod sûre) | `'true'` = panneau dev visible (`window.__env.IS_DEV === 'true'`, STRING, pas booléen) ; vide = masqué |
| `dev-login-enabled` | backend, flag + profil non-prod | absent en prod | `true` en local/dev = `POST /api/auth/dev-login` actif ; prod = absent + garde anti-prod (endpoint inactif même si appelé) |

Fichiers : `payment-platform-ui/src/assets/env.template.js`
(`'${IS_DEV}'`, vide par défaut), `payment-platform-ui/src/assets/env.js`
(local/dev : `'true'`), `deploy/.env.example` (tout vide par défaut).
`src/index.html` charge déjà `assets/env.js` (aucune modif nécessaire).

## 4. Mapping `dev-login-enabled` par profil

| Profil | Flag | `POST /api/auth/dev-login` |
|---|---|---|
| local | `true` | actif (200 + token si username connu, 404 sinon) |
| dev | `true` | actif |
| prod | **absent** + garde anti-prod (profil) | **inactif** (404/403 même si appelé) |

## 5. Mode dev local de bout en bout

1. Backend profil local/dev avec `dev-login-enabled=true` ; front servi
   avec `IS_DEV=true` (`src/assets/env.js` local déjà à `'true'`).
2. Ouvrir `/login` : le panneau dev `[data-testid="dev-login-panel"]`
   est visible (contrat front, agent parallèle).
3. Choisir un utilisateur seedé (ex. `ali.e2e`) → token stocké comme
   le flux password (même `sessionStorage`).
4. Cypress : `npx cypress run --env isDev=true --spec
   "cypress/e2e/20-oauth-devmode.cy.ts"` — `cy.login`/`apiLogin`
   basculent sur dev-login (sans password), `ensureTestUsers`/`getTestCtx`
   inchangés. Sans `--env isDev=true`, la spec 20 est **entièrement
   skippée** (0 test, suite 01–19 verte garantie).

## 6. Garde-fous prod

- `IS_DEV` vide en prod générée (template) → panneau dev impossible.
- `dev-login-enabled` absent en prod + garde anti-prod backend.
- Client IDs seuls : aucun secret OAuth dans le repo, `.env` réel jamais
  commité (`deploy/.env.example` = gabarit vide).
