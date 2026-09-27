# 02 — Matrice de parité Angular → Flutter (référence)

Source : `FRONTEND_ROUTES.md` + `app-routing.module.ts:58-108`. Seuls les 4 rôles mobiles sont portés. SYSTEM_ADMIN volontairement exclu.

Légende : ✅ à porter mobile, ❌ exclu mobile (desktop uniquement).

## Publiques / communes (tous les 4 rôles)

| Route Angular | Écran Flutter | Rôles mobiles | Notes |
|---|---|---|---|
| `/login` | `features/auth/login` | tous | même DTO `{username,password}` |
| `/register` | `features/auth/register` | tous | naît DISABLED (M5) |
| `/setup-password` | `features/auth/setup_password` | tous | |
| `/dashboard` | `features/dashboard` | tous | dispatch : supplier/shop/agent widgets |
| `/dashboard/payments` | `/payments` | tous | paginé B5, filtre org auto JWT |
| `/dashboard/payments/:id` | `/payments/:id` | tous | détail + historique, 403 hors périmètre |
| `/dashboard/payments/stats` | `/payments/stats` | tous | `supplier-summary`, `agent-summary` |
| `/dashboard/scan` | `/scan` | tous | `mobile_scanner`, ouvre payment/order |
| `/dashboard/export` | `/export` | tous | CSV/PDF via `ReportService` |
| `/dashboard/notifications` | `/notifications` | tous | voir doc 03 — FCM + liste + badge |
| `/dashboard/change-password` | `/change-password` | tous | révoque refresh (M1) |
| `/403`, `/404` | `/403`, `/404` | tous | |

## Boutique (SHOP_ADMIN, SHOP_AGENT)

| Route Angular | Écran Flutter | SHOP_ADMIN | SHOP_AGENT | Notes |
|---|---|---|---|---|
| `/dashboard/payments/create` | `/payments/create` | ✅ | ✅ | `Idempotency-Key` auto (B1) |
| `/dashboard/shop/orders` | `/shop/orders` | ✅ | ✅ | |
| `/dashboard/shop/orders/create` | `/shop/orders/create` | ✅ | ✅ | `asapPayment` supporté |
| `/dashboard/shop/orders/:id` | `/shop/orders/:id` | ✅ | ✅ | tunnel confirm/prepare/ready/assign/accept/deliver |
| `/dashboard/shop/disputes/:id` | `/shop/disputes/:id` | ✅ | ✅ | |
| `/dashboard/shop/balance` | `/shop/balance` | ✅ | ✅ | |
| `/dashboard/shop/deliveries` | → `/shop/orders/:id` (onglet livraison) | ✅ | ✅ | même `DeliveryManagementComponent` |

## Fournisseur (SUPPLIER_ADMIN, SUPPLIER_AGENT)

| Route Angular | Écran Flutter | SUPPLIER_ADMIN | SUPPLIER_AGENT | Notes |
|---|---|---|---|---|
| `/dashboard/supplier/agent-payments` | `/supplier/agent-payments` | ✅ | ✅ | |
| `/dashboard/supplier/financial` | `/supplier/financial` | ✅ | ❌ | PWA prioritaire → porter en 1er |
| `/dashboard/supplier/balance` | `/supplier/balance` | ✅ | ❌ | |
| `/dashboard/supplier/stock` | `/supplier/stock` | ✅ | ✅ | mouvements IN/OUT/ADJUSTMENT |
| `/dashboard/supplier/optimization` | `/supplier/optimization` | ✅ | ❌ | ABC/XYZ, EOQ, ROP |
| `/dashboard/supplier/stock/create` | `/supplier/stock/create` | ✅ | ❌ | AddProduct |
| `/dashboard/supplier/categories` | `/supplier/categories` | ✅ | ❌ | soft-delete M3 |
| `/dashboard/supplier/families` | `/supplier/families` | ✅ | ❌ | soft-delete M3 |
| `/dashboard/supplier/products` | `/supplier/products` | ✅ | ❌ | catalogue |
| `/dashboard/supplier/dashboard` | `/supplier/dashboard` | ✅ | ✅ | StockDashboard |
| `/dashboard/supplier/low-stock-alerts` | `/supplier/low-stock-alerts` | ✅ | ❌ | push `stock.low` |
| `/dashboard/supplier/orders` | `/supplier/orders` | ✅ | ✅ | tunnel fournisseur |
| `/dashboard/supplier/orders/create` | `/supplier/orders/create` | ✅ | ❌ | auto-confirmé |
| `/dashboard/supplier/deliveries` | `/supplier/deliveries` | ✅ | ✅ | `accept-delivery` admis ADMIN (spec 19), `deliver` accepte SHOP_ADMIN |

## Exclu mobile (SYSTEM_ADMIN, desktop)

`admin/users`, `admin/users/create`, `sales/accounts`, `admin/suppliers`, `admin/shops`, `admin/relations`, `admin/org-stats`, `admin/audit-logs` → ❌ pas de portage. Si JWT `SYSTEM_ADMIN` détecté sur mobile : écran `/403` + message "utiliser le front desktop".

## API communes (tous portés à l'identique)

Auth M1/B2, users scope org, organisations/catalogue/commandes (tunnel complet + `assign-delivery`, `accept-delivery`, `confirm-delivery`, `deliver`, `accept-asap` idempotent), paiements B1/B5 (`Idempotency-Key`, `confirm|reject|cancel`, `supplier-summary`, `agent-summary`, `auto` interne), notifications (voir doc 03).
