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
4. JWT émis avec `organizationId` et rôles.

## Contrôles à chaque requête (chaque service)

| # | Vérification | Échec |
|---|---|---|
| 1 | JWT signature + expiration + `jti` inconnu (pas de revocation list, cf. note) | 401 |
| 2 | `user.status = ACTIVE` (cache court + re-vérification synchrone Identity pour les actions critiques) | 403 |
| 3 | `organization.status = ACTIVE` | 403 |
| 4 | permission RBAC (`@PreAuthorize`) | 403 |
| 5 | scope ressource (`organizationId` du token vs ressource demandée) | 403 |

> Note revocation : à ce stade, un utilisateur désactivé est bloqué car le service re-vérifie le statut en base à chaque requête critique. La liste de révocation (déni) est une évolution documentée.

## Sécurité applicative

- Validation stricte des DTO (`spring-boot-starter-validation`) : email, regex username, longueurs, montant positif.
- Pas de secrets dans le repo : `.env.example` + variables d'environnement ; `.gitignore` exclut `.env`.
- Headers : `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, CSP de base ; CORS limité à l'origine Angular.
- Rate limiting : filtre Gateway (bucket par IP + par utilisateur) — 120 req/min.
- Correlation ID : `X-Correlation-Id` généré au Gateway, propagé aux services et dans les logs (MDC).
- JSON des erreurs sans stacktrace ; logs structurés sans données sensibles (jamais de passwordHash dans les logs).
- Audit complet de toute action d'écriture (voir business-rules.md §8).