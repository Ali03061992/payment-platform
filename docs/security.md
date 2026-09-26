# Sécurité

## Modèle

```
Angular ──JWT──▶ API Gateway ──JWT──▶ Microservice ──autorisation──▶ ressource
                    │                     │
                    │ 1. signature         │ 1. signature + exp
                    │ 2. expiration        │ 2. user ACTIVE ∧ org ACTIVE
                    │ 3. CORS / rate-limit │ 3. permission RBAC (méthode)
                    │ 4. correlation-id    │ 4. scope ressource (organizationId du JWT)
```

**Le Gateway n'est pas une frontière de confiance** : chaque service re-valide le JWT (Nimbus/JWS) et l'état du compte au moment de la requête.

## JWT

Claims : `sub (userId)`, `username`, `roles[]`, `organizationId`, `iat`, `exp` (30 min), `jti`.
Signé **HS256**, secret partagé injecté par variable d'environnement `JWT_SECRET` (≥ 32 octets). Passwords **BCrypt** (cost 12).

## RBAC — permissions par rôle

| Permission | SYSTEM_ADMIN | SUPPLIER_ADMIN | SUPPLIER_AGENT | SHOP_ADMIN | SHOP_AGENT |
|---|:-:|:-:|:-:|:-:|:-:|
| `ADMIN_MANAGE_ORGANIZATIONS` | ✔ | | | | |
| `ADMIN_MANAGE_USERS` | ✔ | | | | |
| `ADMIN_VIEW_AUDIT` | ✔ | | | | |
| `ADMIN_VIEW_STATS` | ✔ | | | | |
| `SUPPLIER_MANAGE_AGENTS` | | ✔ | | | |
| `SUPPLIER_MANAGE_PAYMENTS` | | ✔ | ✔ | | |
| `SHOP_MANAGE_AGENTS` | | | | ✔ | |
| `SHOP_CREATE_PAYMENTS` | | | | ✔ | ✔ |
| `SHOP_CANCEL_PAYMENTS` | | | | ✔ | ✔ |
| `VIEW_PAYMENTS` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `VIEW_NOTIFICATIONS` | ✔ | ✔ | ✔ | ✔ | ✔ |

Implémentation : `@PreAuthorize("hasAuthority('...')")` + garde de scope (`supplierId == jwt.organizationId`) dans chaque use case. Tests de sécurité dédiés.

## Contrôles au login (Identity)

1. utilisateur existe ;
2. `user.status = ACTIVE` **et** `organization.status = ACTIVE` (requête au Organization Service via Gateway) ;
3. mot de passe vérifié (BCrypt) ;
4. JWT émis avec `organizationId` et rôles **+ refresh token opaque émis** (M1, TTL 7 j, hash SHA-256 seul persisté).
5. **M5** : un compte auto-inscrit naît `DISABLED` (`RegisterUseCase.java:78-82`) — login impossible avant validation admin (`PATCH /api/users/{id}/activate`).

## Idempotence paiements (B1)

`Idempotency-Key` persisté (`payments.idempotency_key`, index unique `V3__add_idempotency_key.sql`),
vérifié dans `CreatePaymentUseCase` : double POST identique = 1 seul paiement.
Front : clé générée par payload (`PaymentService.newIdempotencyKey`), réutilisée au retry.
Paiement auto ASAP : clé `asap-<orderId>` portée par `POST /api/internal/payments/auto`.

## Rate-limit auth strict (B2)

`RateLimitFilter` : bucket **10/min/IP** sur `login/register/refresh/logout`
(`RATE_LIMIT_AUTH_PER_MINUTE`, défaut 10 en prod/compose, **1000 en local/E2E** via
`application-local.yml` et `.run/5_API_Gateway.run.xml` pour ne pas flaker la suite) ;
bucket général 120/min conservé. Réponse 429 : `Retry-After: 60` + `X-RateLimit-*`.
Preuve déterministe : `RateLimitFilterTest` (11e requête → 429) ; câblage prouvé en E2E
par les headers (`15-rate-limit-auth.cy.ts` — pas de hammering en suite, ~70 logins/IP).

## Gateway deny-by-default + secret interne (B3)

- Plus de `"/api/**".permitAll()` : `GatewaySecurityConfig` n'ouvre que
  `/api/auth/login|register|refresh|logout`, `/api/auth/password-setup/**`,
  `/actuator/health|info`, swagger ; tout le reste `.authenticated()`.
- `JwtValidationFilter` alimente le `SecurityContext` (JWT + permissions du
  `PermissionCatalog`) ; sans JWT → **401 JSON unifié** (`Token d'authentification manquant`),
  prouvé en E2E (`16-gateway-security.cy.ts`).
- `internal/**` exige **JWT (gateway) + `X-Internal-Token` (service appelé)** —
  défense en profondeur ; manquant OU invalide → 401 unifié (`UnauthorizedException`).
- `INTERNAL_SECRET` **fail-fast au boot** (`InternalSecretValidator.requireValid`) :
  défauts `dev-...` en local/dev uniquement, **aucun défaut en prod**
  (`application-prod.yml: ${INTERNAL_SECRET}` sans fallback).

## Refresh tokens (M1)

Table `refresh_tokens` (V6, Liquibase) : `token_hash` (SHA-256, unique), `user_id`,
`expires_at`, `revoked`, `replaced_by`. Rotation à chaque `POST /refresh`
(ancien marqué `replaced_by`), révocation idempotente au `POST /logout`,
révocation totale au changement de mot de passe / désactivation
(`ChangePasswordUseCase`, `UserStatusUseCase`). Front : refresh silencieux
**single-flight** dans `JwtInterceptor` (`refreshInFlight` + `shareReplay(1)`,
URLs auth exclues anti-boucle).

## Contrôles à chaque requête (chaque service)

| # | Vérification | Échec |
|---|---|---|
| 1 | JWT signature + expiration + `jti` inconnu (pas de revocation list, cf. note) | 401 |
| 2 | `user.status = ACTIVE` (cache court + re-vérification synchrone Identity pour les actions critiques) | 403 |
| 3 | `organization.status = ACTIVE` | 403 |
| 4 | permission RBAC (`@PreAuthorize`) | 403 |
| 5 | scope ressource (`organizationId` du token vs ressource demandée) | 403 |

> Note revocation : les refresh tokens sont révoqués en base (M1) ; côté access-JWT (30 min),
> un utilisateur désactivé est bloqué car le service re-vérifie le statut en base à chaque requête critique.

## Sécurité applicative

- Validation stricte des DTO (`spring-boot-starter-validation`) : email, regex username, longueurs, montant positif.
- Pas de secrets dans le repo : `.env.example` + variables d'environnement ; `.gitignore` exclut `.env`.
- Headers : `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, CSP de base ; CORS limité à l'origine Angular.
- Rate limiting : `RateLimitFilter` — 120 req/min général, **10 req/min/IP sur auth** (B2) ; assoupli à 1000 en local/E2E (`RATE_LIMIT_AUTH_PER_MINUTE`).
- Correlation ID : `X-Correlation-Id` généré au Gateway, propagé aux services et dans les logs (MDC).
- JSON des erreurs sans stacktrace ; logs structurés sans données sensibles (jamais de passwordHash dans les logs).
- Audit complet de toute action d'écriture (voir business-rules.md §8).