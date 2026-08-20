# API REST

Point d'entrée unique : **API Gateway** (port 8081). Convention : JSON, `X-Correlation-Id` en entrée/sortie, erreurs uniformes :

```json
{ "timestamp": "...", "status": 409, "error": "CONFLICT", "message": "...", "path": "..." }
```

Code d'erreur métier via `message` + `error`. OpenAPI exposé par chaque service (`/v3/api-docs`) et agrégé au Gateway.

## Auth (Identity)

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | `{username, password}` → `{accessToken, tokenType, expiresIn, user}` |
| POST | `/api/auth/refresh` | public | `{refreshToken}` → nouveau token (si activé) |
| GET | `/api/auth/me` | auth | profil courant + rôles + statut effectif |

## Utilisateurs & agents (Identity)

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| GET | `/api/users` | SYSTEM_ADMIN | liste des utilisateurs (filtres : org, rôle, statut) |
| GET | `/api/users/{id}` | SYSTEM_ADMIN, admin de l'org | détail |
| PATCH | `/api/users/{id}/activate` | SYSTEM_ADMIN, admin de l'org | activation |
| PATCH | `/api/users/{id}/disable` | SYSTEM_ADMIN, admin de l'org | désactivation individuelle |
| GET | `/api/suppliers/{supplierId}/agents` | SUPPLIER_ADMIN (org = supplierId) | agents du fournisseur |
| POST | `/api/suppliers/{supplierId}/agents` | SUPPLIER_ADMIN (org = supplierId) | créer agent `{firstName, lastName, email, phone, username, role}` |
| PATCH | `/api/suppliers/{supplierId}/agents/{agentId}` | SUPPLIER_ADMIN (org = supplierId) | modifier agent |
| PATCH | `/api/suppliers/{supplierId}/agents/{agentId}/activate` | SUPPLIER_ADMIN | activer agent |
| PATCH | `/api/suppliers/{supplierId}/agents/{agentId}/disable` | SUPPLIER_ADMIN | désactiver agent |

## Organisations (Organization)

| Méthode | Path | Rôle | Description |
|---|---|---|---|
| GET | `/api/admin/suppliers` | SYSTEM_ADMIN | liste fournisseurs (+ nb agents, nb boutiques) |
| POST | `/api/admin/suppliers` | SYSTEM_ADMIN | `{name, adminEmail, adminUsername, ...}` crée org + compte SUPPLIER_ADMIN |
| GET | `/api/admin/suppliers/{id}` | SYSTEM_ADMIN | détail |
| PATCH | `/api/admin/suppliers/{id}` | SYSTEM_ADMIN | modifier (nom) |
| PATCH | `/api/admin/suppliers/{id}/activate` | SYSTEM_ADMIN | activation |
| PATCH | `/api/admin/suppliers/{id}/disable` | SYSTEM_ADMIN | désactivation + cascade comptes |
| GET | `/api/admin/shops` | SYSTEM_ADMIN | liste boutiques |
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
| GET | `/api/payments` | tous | liste filtrable (statut, fournisseur, boutique, période) — toujours filtrée par org du JWT |
| POST | `/api/payments` | SHOP_ADMIN/SHOP_AGENT | `{supplierId, amount, currency, reference?, comment?}` → `PENDING` |
| GET | `/api/payments/{id}` | acteurs du shop/supplier concerné | détail + historique |
| POST | `/api/payments/{id}/confirm` | SUPPLIER_ADMIN/SUPPLIER_AGENT | PENDING → CONFIRMED (409 si conflit) |
| POST | `/api/payments/{id}/reject` | SUPPLIER_ADMIN/SUPPLIER_AGENT | `{rejectionReason}` PENDING → REJECTED |
| POST | `/api/payments/{id}/cancel` | SHOP_ADMIN/SHOP_AGENT | PENDING → CANCELLED |

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

- `POST /api/payments` accepte un `Idempotency-Key` (retourne le paiement existant si déjà traité).
- Toute mise à jour concurrente → `409 CONFLICT` (optimistic locking).
- Validation relation inexistante → `422 UNPROCESSABLE_ENTITY`.
- Accès hors périmètre → `403 FORBIDDEN`.