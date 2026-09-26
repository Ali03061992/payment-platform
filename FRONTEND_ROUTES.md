# Frontend Routes & API Paths - Payment Platform

> Vérifié le 25/09/2026 depuis `payment-platform-ui/src/app/app-routing.module.ts:58-108`,
> `layout.component.ts:33-62`, `core/role.guard.ts:9-36`.
> L'ancienne version de ce document ne listait que ~10 routes : elle est remplacée par
> l'inventaire réel ci-dessous (41 URLs applicatives + redirects).

## Angular Routes (41 URLs réelles)

Guards : `AuthGuard` sur `/dashboard` (tout le layout) + `RoleGuard` sur chaque route
fille à `data.roles` (refus → redirect `/403`, pas de redirect silencieux — cf. M4).
`AuthGuard` détecte le JWT expiré côté client. `**` → redirect `/404`.

### Publiques (hors layout)

| Route | Component | Guard | Description |
|---|---|---|---|
| `/login` | LoginComponent | — | Connexion |
| `/register` | RegisterComponent | — | Auto-inscription (M5 : naît DISABLED, validation admin) |
| `/setup-password` | PasswordSetupComponent | — | Initialisation mot de passe |
| `/403` | ForbiddenComponent | — | M4 : refus RoleGuard |
| `/404` | NotFoundComponent | — | Wildcard `**` |

### Layout `/dashboard` (AuthGuard, `LayoutComponent`)

| Route complète | Component | `data.roles` | Description |
|---|---|---|---|
| `/dashboard` | DashboardComponent | (hérite AuthGuard) | Tableau de bord (dispatch par rôle) |
| `/dashboard/admin/users` | UserManagementComponent | SYSTEM_ADMIN | Liste/gestion utilisateurs (paginée B5) |
| `/dashboard/admin/users/create` | CreateUserComponent | SYSTEM_ADMIN | Créer un compte |
| `/dashboard/sales/accounts` | AccountActivationComponent | SYSTEM_ADMIN | Validation comptes (workflow M5) |
| `/dashboard/admin/suppliers` | SupplierManagementComponent | SYSTEM_ADMIN | Fournisseurs |
| `/dashboard/admin/shops` | ShopManagementComponent | SYSTEM_ADMIN | Boutiques |
| `/dashboard/admin/relations` | RelationManagementComponent | SYSTEM_ADMIN | Relations fournisseur↔boutique |
| `/dashboard/admin/org-stats` | OrganizationStatsComponent | SYSTEM_ADMIN | Stats organisations |
| `/dashboard/admin/audit-logs` | AuditLogManagementComponent | SYSTEM_ADMIN | Journal d'audit (paginé) |
| `/dashboard/payments` | PaymentListComponent | tous (allRoles) | Liste paiements |
| `/dashboard/payments/create` | CreatePaymentComponent | SYSTEM_ADMIN, SHOP_ADMIN, SHOP_AGENT | Créer paiement (Idempotency-Key auto, B1) |
| `/dashboard/payments/stats` | PaymentStatsComponent | tous | Stats paiements |
| `/dashboard/payments/:id` | PaymentDetailComponent | tous | Détail + historique |
| `/dashboard/scan` | QrScannerComponent | tous | Scanner QR |
| `/dashboard/export` | ExportComponent | tous | Export CSV/PDF |
| `/dashboard/supplier/agent-payments` | AgentPaymentsComponent | SUPPLIER_ADMIN, SUPPLIER_AGENT | Paiements agents |
| `/dashboard/supplier/financial` | SupplierFinancialComponent | SUPPLIER_ADMIN | Finance fournisseur (PWA prioritaire) |
| `/dashboard/supplier/balance` | SupplierBalanceComponent | SUPPLIER_ADMIN | Balance fournisseur |
| `/dashboard/supplier/stock` | StockManagementComponent | SUPPLIER_ADMIN, SUPPLIER_AGENT | Stock |
| `/dashboard/supplier/optimization` | StockOptimizationComponent | SUPPLIER_ADMIN | Optimisation stock |
| `/dashboard/supplier/stock/create` | AddProductComponent | SUPPLIER_ADMIN | Ajouter produit |
| `/dashboard/supplier/categories` | CategoryManagementComponent | SUPPLIER_ADMIN | Catégories (soft-delete M3) |
| `/dashboard/supplier/families` | FamilyManagementComponent | SUPPLIER_ADMIN | Familles (soft-delete M3) |
| `/dashboard/supplier/products` | ProductManagementComponent | SUPPLIER_ADMIN | Catalogue produits |
| `/dashboard/supplier/dashboard` | StockDashboardComponent | SUPPLIER_ADMIN, SUPPLIER_AGENT | Dashboard stock |
| `/dashboard/supplier/low-stock-alerts` | LowStockAlertsComponent | SUPPLIER_ADMIN | Alertes stock bas |
| `/dashboard/supplier/orders` | OrderManagementComponent | SUPPLIER_ADMIN, SUPPLIER_AGENT | Commandes fournisseur |
| `/dashboard/supplier/orders/create` | SupplierCreateOrderComponent | SUPPLIER_ADMIN | Créer commande (fournisseur) |
| `/dashboard/supplier/deliveries` | DeliveryManagementComponent | SUPPLIER_ADMIN, SUPPLIER_AGENT | Livraisons — admin fournisseur livre aussi (spec 19) ; sélecteur destinataire inclut les SHOP_ADMIN |
| `/dashboard/shop/orders` | OrderListComponent | SHOP_ADMIN, SHOP_AGENT | Commandes boutique |
| `/dashboard/shop/orders/create` | CreateOrderComponent | SHOP_ADMIN, SHOP_AGENT | Créer commande (+ `asapPayment`) |
| `/dashboard/shop/orders/:id` | ShopOrderDetailComponent | SHOP_ADMIN, SHOP_AGENT | Détail commande boutique |
| `/dashboard/shop/disputes/:id` | DisputeDetailComponent | SHOP_ADMIN, SHOP_AGENT | Litige commande |
| `/dashboard/shop/balance` | BalanceViewComponent | SHOP_ADMIN, SHOP_AGENT | Balance boutique |
| `/dashboard/notifications` | NotificationsComponent | tous | Notifications (SSE + FCM) |
| `/dashboard/change-password` | ChangePasswordComponent | (hérite AuthGuard, sans RoleGuard) | Changement mot de passe (révoque refresh M1) |

## Rôles front (M4 — purge SHOP_MANAGER)

`adminRoles = ['SYSTEM_ADMIN']`, `supplierRoles = ['SUPPLIER_ADMIN','SUPPLIER_AGENT']`,
`shopRoles = ['SHOP_ADMIN','SHOP_AGENT']`, `allRoles` = union.
Nav (`layout.component.ts:33-62`) : **aucune entrée SHOP_MANAGER** — purge vérifiée.
Backend `OrderController` accepte encore `SHOP_MANAGER` dans ses `@PreAuthorize`
(compatibilité historique, sans effet front).

| Role | Permissions (PermissionCatalog) |
|---|---|
| SYSTEM_ADMIN | ADMIN_MANAGE_ORGANIZATIONS, ADMIN_MANAGE_USERS, ADMIN_VIEW_AUDIT, ADMIN_VIEW_STATS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SUPPLIER_ADMIN | SUPPLIER_MANAGE_AGENTS, SUPPLIER_MANAGE_PAYMENTS, SUPPLIER_MANAGE_PRODUCTS, SUPPLIER_MANAGE_STOCK, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SUPPLIER_AGENT | SUPPLIER_MANAGE_PAYMENTS, SUPPLIER_MANAGE_STOCK, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SHOP_ADMIN | SHOP_MANAGE_AGENTS, SHOP_CREATE_PAYMENTS, SHOP_CANCEL_PAYMENTS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SHOP_AGENT | SHOP_CREATE_PAYMENTS, SHOP_CANCEL_PAYMENTS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |

> `SALES` (ancien tableau) n'existe plus ni en nav ni en routing : `sales/accounts`
> est réservé SYSTEM_ADMIN.

## Backend API Endpoints (via gateway http://localhost:8081)

### Auth (Identity) — M1/B2

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public (rate-limit strict B2) | `{username,password}` → `{accessToken, refreshToken, expiresIn, user}` |
| POST | `/api/auth/refresh` | public (rate-limit strict B2) | `{refreshToken}` → rotation, ancien révoqué (`AuthController.java:38-41`) |
| POST | `/api/auth/logout` | public (rate-limit strict B2) | `{refreshToken}` → révocation idempotente, 204 (`AuthController.java:43-47`) |
| POST | `/api/auth/register` | public (rate-limit strict B2) | M5 : crée DISABLED, `organizationId=null`, à valider |
| POST | `/api/auth/change-password` | auth | Change MDP + révoque tous les refresh (`ChangePasswordUseCase`) |
| GET | `/api/auth/me` | auth (gateway JWT) | Profil courant + rôles + statut effectif |

### Users (Identity) — B5 paginé

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users?page=0&size=20` | ADMIN_MANAGE_USERS | Enveloppe `{items,totalElements,totalPages,number}`, size plafonné à 100 (`UserController.java:37-47`) |
| GET | `/api/users/{id}` | auth (scope org) | Détail |
| POST | `/api/users` | ADMIN_MANAGE_USERS | Création admin |
| PATCH | `/api/users/{id}/activate` | ADMIN_MANAGE_USERS | Validation compte M5 |
| PATCH | `/api/users/{id}/disable` | ADMIN_MANAGE_USERS | Désactivation (révoque refresh M1) |

### Interne Identity (JWT gateway + `X-Internal-Token`, B3)

| Method | Path | Description |
|---|---|---|
| POST | `/api/internal/users` | Création inter-services |
| GET | `/api/internal/users/{id}` | Détail interne |
| GET | `/api/internal/users?organizationId=` | **Nouveau** : résout les destinataires livraison dont SHOP_ADMIN (`InternalUserController.java:54-62`, `IdentityClient.java:70`) |

### Organisations / catalogue / commandes (Organization)

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/suppliers?page=&size=` | Paginé B5, max 100 |
| GET | `/api/admin/shops?page=&size=` | Paginé B5, max 100 |
| CRUD | `/api/suppliers/{id}/products`, `/movements`, `/categories`, `/families` | M3 : DELETE = soft-delete (`deleted_at`, V10) |
| POST | `/api/orders` | `{supplierId, shopId, items[], asapPayment, currency}` |
| POST | `/api/orders/{id}/confirm\|prepare\|ready` | Tunnel fournisseur |
| POST | `/api/orders/{id}/assign-delivery` | `{agentId, plannedDeliveryDate}` |
| POST | `/api/orders/{id}/accept-delivery` | `{accepted}` — **SUPPLIER_ADMIN admis** (spec 19) |
| POST | `/api/orders/{id}/confirm-delivery` | `{confirmedDate}` |
| POST | `/api/orders/{id}/deliver` | `{receivedBy}` — **accepte un SHOP_ADMIN** (spec 19) |
| POST | `/api/orders/{id}/accept-asap` | Idempotent, ne recrée pas le paiement auto |

### Paiements (Payment) — B1/B5 + ASAP

| Method | Path | Description |
|---|---|---|
| GET | `/api/payments?page=0&size=50` | Enveloppe paginée (filtre auto par org du JWT) |
| POST | `/api/payments` | Header `Idempotency-Key` persisté + contrainte unique V3 (`PaymentController.java:61-69`) |
| GET | `/api/payments/{id}`, `/reference/{ref}` | Détail (403 hors périmètre) |
| POST | `/api/payments/{id}/confirm\|reject\|cancel` | 409 conflit vs 503 infra (M2) |
| GET | `/api/payments/supplier-summary?supplierId=` | **Agrégats SQL** (pas de full-load, B5) : `{pendingTotal,pendingCount,confirmedTotal,confirmedCount}` |
| GET | `/api/payments/agent-summary?supplierId=&from=&to` | Synthèse agents |
| POST | `/api/internal/payments/auto` | **Nouveau** : headers `X-Internal-Token` + `X-Actor-User-Id`, clé `asap-<orderId>` (`InternalPaymentController.java:33-52`) |

### Notifications

| Method | Path | Description |
|---|---|---|
| GET | `/api/notifications?page=&size=` | Paginées `{items,totalElements,totalPages,number}` |
| PATCH | `/api/notifications/{id}/read` | Marquer lue |
| GET | `/api/notifications/stream?token=` | SSE (JWT en query param accepté par `JwtValidationFilter`) |
| GET | `/api/notifications/unread-count` | Compteur |

## Services & ports (local IDE)

| Service | Port | Accès local |
|---|---|---|
| Angular (`ng serve`) | **4200** | http://localhost:4200 (dev) ; 8080 = nginx docker |
| API Gateway | 8081 | http://localhost:8081 |
| Identity | 8082 | via gateway |
| Organization | 8083 | via gateway |
| Payment | 8084 | via gateway |
| Notification | 8085 | via gateway |

Voir `deploy/LOCAL_DEV.md` (profils `local`, configs `.run/`) et `CYPRESS_E2E_GUIDE.md`.
