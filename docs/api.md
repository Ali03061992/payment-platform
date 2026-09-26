# API REST

Point d'entrée unique : **API Gateway** (port 8081). Convention : JSON, `X-Correlation-Id` en entrée/sortie, erreurs uniformes :

```json
{ "timestamp": "...", "status": 409, "error": "CONFLICT", "message": "...", "path": "..." }
```

Code d'erreur métier via `message` + `error`. OpenAPI exposé par chaque service (`/v3/api-docs`) et agrégé au Gateway.

## Auth (Identity) — M1 refresh rotation + B2 rate-limit

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public (rate-limit 10/min/IP, `Retry-After: 60`) | `{username, password}` → `{accessToken, refreshToken, expiresIn, user}` |
| POST | `/api/auth/refresh` | public (rate-limit 10/min/IP) | `{refreshToken}` → rotation : nouveau couple access+refresh, ancien révoqué (`replaced_by`), 401 si révoqué/expiré/compte désactivé |
| POST | `/api/auth/logout` | public (rate-limit 10/min/IP) | `{refreshToken}` → révocation idempotente (token inconnu = no-op), 204 |
| POST | `/api/auth/register` | public (rate-limit 10/min/IP) | M5 : crée un compte **DISABLED** sans organisation, à valider par admin |
| POST | `/api/auth/change-password` | auth | change le MDP + révoque **tous** les refresh du user |
| GET | `/api/auth/me` | auth | profil courant + rôles + statut effectif |

## Utilisateurs & agents (Identity) — B5 paginé

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| GET | `/api/users?page=0&size=20` | SYSTEM_ADMIN | **paginé** : enveloppe `{items, totalElements, totalPages, number}`, `size` plafonné à 100 (filtres : `organizationId`, `role`, `statusFilter`) |
| GET | `/api/users/{id}` | SYSTEM_ADMIN, admin de l'org | détail |
| PATCH | `/api/users/{id}/activate` | SYSTEM_ADMIN, admin de l'org | activation (= validation M5) |
| PATCH | `/api/users/{id}/disable` | SYSTEM_ADMIN, admin de l'org | désactivation individuelle + révocation refresh (M1) |
| GET | `/api/suppliers/{supplierId}/agents` | SUPPLIER_ADMIN (org = supplierId) | agents du fournisseur |
| POST | `/api/suppliers/{supplierId}/agents` | SUPPLIER_ADMIN (org = supplierId) | créer agent `{firstName, lastName, email, phone, username, role}` |
| PATCH | `/api/suppliers/{supplierId}/agents/{agentId}` | SUPPLIER_ADMIN (org = supplierId) | modifier agent |
| PATCH | `/api/suppliers/{supplierId}/agents/{agentId}/activate` | SUPPLIER_ADMIN | activer agent |
| PATCH | `/api/suppliers/{supplierId}/agents/{agentId}/disable` | SUPPLIER_ADMIN | désactiver agent |

## Organisations (Organization)

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| GET | `/api/admin/suppliers?page=0&size=20` | SYSTEM_ADMIN | **paginé B5** (max 100) : `{items, totalElements, totalPages, number}` |
| POST | `/api/admin/suppliers` | SYSTEM_ADMIN | `{name, adminEmail, adminUsername, ...}` crée org + compte SUPPLIER_ADMIN |
| GET | `/api/admin/suppliers/{id}` | SYSTEM_ADMIN | détail |
| PATCH | `/api/admin/suppliers/{id}` | SYSTEM_ADMIN | modifier (nom) |
| PATCH | `/api/admin/suppliers/{id}/activate` | SYSTEM_ADMIN | activation |
| PATCH | `/api/admin/suppliers/{id}/disable` | SYSTEM_ADMIN | désactivation + cascade comptes |
| GET | `/api/admin/shops?page=0&size=20` | SYSTEM_ADMIN | **paginé B5** (max 100) |
| POST | `/api/admin/shops` | SYSTEM_ADMIN | `{name, adminEmail, ...}` crée org + compte SHOP_ADMIN |
| PATCH | `/api/admin/shops/{id}/activate` | SYSTEM_ADMIN | activation |
| PATCH | `/api/admin/shops/{id}/disable` | SYSTEM_ADMIN | désactivation + cascade comptes |
| GET | `/api/shops` | SHOP_ADMIN/AGENT | boutiques liées à mon org |
| GET | `/api/suppliers` | SUPPLIER_ADMIN/AGENT, SHOP_ADMIN | fournisseurs (liés à ma boutique pour les shops) |
| GET | `/api/suppliers/{id}/shops` | SUPPLIER_ADMIN | boutiques liées à mon fournisseur |
| POST | `/api/suppliers/{supplierId}/shops/{shopId}/link` | SYSTEM_ADMIN | créer relation (idempotent) |
| DELETE | `/api/suppliers/{supplierId}/shops/{shopId}/link` | SYSTEM_ADMIN | supprimer relation |

## Paiements (Payment)

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| GET | `/api/payments?page=0&size=50` | tous | **paginé B5** `{items,totalElements,totalPages,number}` — toujours filtrée par org du JWT |
| POST | `/api/payments` | SHOP_ADMIN/SHOP_AGENT | `{supplierId, amount, currency, reference?, comment?}` → `PENDING` — header `Idempotency-Key` **persisté + contrainte unique** (B1, doublon = paiement existant) |
| GET | `/api/payments/{id}` | acteurs du shop/supplier concerné | détail + historique |
| POST | `/api/payments/{id}/confirm` | SUPPLIER_ADMIN/SUPPLIER_AGENT | PENDING → CONFIRMED (409 si conflit) |
| POST | `/api/payments/{id}/reject` | SUPPLIER_ADMIN/SUPPLIER_AGENT | `{rejectionReason}` PENDING → REJECTED |
| POST | `/api/payments/{id}/cancel` | SHOP_ADMIN/SHOP_AGENT | PENDING → CANCELLED |
| GET | `/api/payments/supplier-summary?supplierId=` | SUPPLIER_ADMIN (scope org) | **B5 agrégats SQL** (aucun full-load) : `{pendingTotal, pendingCount, confirmedTotal, confirmedCount}` |
| POST | `/api/internal/payments/auto` | interne (JWT gateway + `X-Internal-Token` + `X-Actor-User-Id`) | **ASAP** : crée le paiement auto à la livraison, idempotence `asap-<orderId>` (`InternalPaymentController.java:33-52`) |

## Interne Identity (B3 : JWT gateway + secret partagé, 401 unifié)

| Méthode | Path | Description |
|---|---|---|
| POST | `/api/internal/users` | création inter-services (`X-Internal-Token`) |
| GET | `/api/internal/users/{id}` | détail interne |
| GET | `/api/internal/users?organizationId=` | liste les users d'une org — sert le sélecteur « reçu par » (inclut SHOP_ADMIN, spec 19) |

## Notifications (Notification)

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| GET | `/api/notifications` | tous | mes notifications (paginées, filtre lu/non lu) |
| PATCH | `/api/notifications/{id}/read` | propriétaire | marquer lue |
| GET | `/api/notifications/stream` | tous (auth SSE) | flux temps réel `text/event-stream` |
| GET | `/api/notifications/unread-count` | tous | compteur non lues |

## Administration (cross-service, SYSTEM_ADMIN)

| Méthode | Path | Service | Description |
|---|---|---|---|
| GET | `/api/admin/audit` | agrégé | audit (par service) |
| GET | `/api/admin/stats` | agrégé | statistiques globales (orgs, users, paiements par statut) |

## Idempotence & conflits

- `POST /api/payments` accepte un `Idempotency-Key` **persisté** (`payments.idempotency_key`, index unique V3) : double POST identique = 1 seul paiement (B1).
- `POST /api/internal/payments/auto` porte la clé `asap-<orderId>` : rejeu livraison + `accept-asap` = paiement existant, pas de doublon.
- Listes `users/orgs/payments/notifications` : `?page=&size=`, `size` plafonné à **100**, enveloppes `{items, totalElements, totalPages, number}` (B5).
- Toute mise à jour concurrente → `409 CONFLICT` (optimistic locking).
- Validation relation inexistante → `422 UNPROCESSABLE_ENTITY`.
- Accès hors périmètre → `403 FORBIDDEN`.