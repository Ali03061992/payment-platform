# 01 — Architecture mobile Flutter

## Décision

Flutter unique (Android + iOS), une seule app multi-rôles avec guards. Pas de SYSTEM_ADMIN sur mobile (gestion org/users/audit/stats reste Angular desktop).

## Stack

- Flutter 3.35+, Dart 3.9+, Riverpod (`flutter_riverpod`), Dio, go_router, `flutter_secure_storage` + `shared_preferences`, `firebase_messaging` + `flutter_local_notifications`, `mobile_scanner` (QR), `hive` (cache offline), `pdf`/`share_plus` (export).
- Backend : gateway uniquement (`API_BASE_URL`, défaut `http://localhost:8081`). Chemins `/api/...` identiques au front.

## Structure

```
payment-platform-mobile/
  lib/
    main.dart                 bootstrap + Firebase init + providers
    app.dart                  MaterialApp.router
    core/
      api/{dio_client.dart, api_exception.dart, paged_response.dart}
      auth/{auth_storage.dart, auth_interceptor.dart, auth_provider.dart, role_guard.dart}
      router/{app_router.dart, deep_link_mapper.dart}
      notifications/{fcm_service.dart, notification_api.dart, notification_providers.dart, notification_channel.dart}
      theme/app_theme.dart
      offline/{cache_service.dart, outbox_queue.dart}
    features/
      auth/{login, register, setup_password, change_password}
      dashboard/              dispatch par rôle
      payments/{list, detail, create, stats}
      supplier/{stock_dashboard, stock, add_product, products, categories, families, optimization, low_stock_alerts, orders, create_order, deliveries, agent_payments, financial, balance}
      shop/{orders, create_order, order_detail, dispute_detail, balance}
      scan/                   mobile_scanner -> /payments/:id ou /shop/orders/:id
      export/                 CSV/PDF via ReportService
      notifications/          liste + filtre + badge
    shared/widgets/{app_scaffold, role_nav, paged_list, empty_state, confirm_dialog}
```

## Auth

- `POST /api/auth/login` → `{accessToken, refreshToken, expiresIn, user}`. Stockage secure tokens, cache user JSON.
- Dio interceptor : `Authorization: Bearer`, refresh auto sur 401 via `POST /api/auth/refresh` (rotation, ancien révoqué), logout sur échec (`POST /api/auth/logout` + `DELETE /api/fcm-tokens` + clear).
- `GET /api/auth/me` au boot pour resync rôle/statut. `POST /api/auth/register` (naît DISABLED), `POST /api/auth/change-password` (révoque refresh).

## Router (go_router)

- Publiques : `/login`, `/register`, `/setup-password`, `/403`, `/404`.
- Protégées `/` (ShellRoute bottom-nav + drawer par rôle) :
  - tous : `/`, `/payments`, `/payments/create` (sauf SUPPLIER_AGENT), `/payments/stats`, `/payments/:id`, `/scan`, `/export`, `/notifications`, `/change-password`
  - supplier : `/supplier/agent-payments`, `/supplier/financial` (ADMIN), `/supplier/balance` (ADMIN), `/supplier/stock`, `/supplier/optimization` (ADMIN), `/supplier/stock/create` (ADMIN), `/supplier/categories|families|products` (ADMIN), `/supplier/dashboard`, `/supplier/low-stock-alerts` (ADMIN), `/supplier/orders`, `/supplier/orders/create` (ADMIN), `/supplier/deliveries`
  - shop : `/shop/orders`, `/shop/orders/create`, `/shop/orders/:id`, `/shop/disputes/:id`, `/shop/balance`
- Guards : `AuthGuard` (token valide, sinon `/login`), `RoleGuard` (rôle dans liste, sinon `/403`). Conversion deep-link FCM : `/dashboard/X` → `/X` (`deep_link_mapper.dart`).

## Offline / perf

- Lecture : cache Hive 5 min des listes (payments, orders, stock), pull-to-refresh + pagination B5 (`page/size`, `size<=100`).
- Écriture : `Idempotency-Key` UUID sur `POST /api/payments`, outbox Hive pour create payment/order en offline avec retry + dédup.
- Pas de SSE en background (batterie). Polling `unread-count` 30 s max en foreground si FCM absent.

## Sécurité

- Jamais de token en log, `flutter_secure_storage` (Keychain/Keystore), cert pinning en prod, JWT expiré → refresh puis login, `SYSTEM_ADMIN` refusé côté mobile même si backend le renvoie.
