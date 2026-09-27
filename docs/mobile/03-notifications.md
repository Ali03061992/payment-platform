# 03 — Notifications mobiles (prioritaire)

## État existant (à réutiliser, ne pas réinventer)

- Backend : `NotificationController` (`GET /api/notifications?page&size&type` → `{items,totalElements,totalPages,currentPage,size}`, `GET /unread-count` → `{count}`, `POST /read-all` → `{updated}`, `POST /{id}/read` → 204, `GET /stream?token=` SSE avec JWT HMAC, registre `user:` + `org:`), `FcmTokenController` (`POST/DELETE /api/fcm-tokens {token}`), `PushNotificationService` (`app.push.enabled`, payload notif title/body + data `{tag,url}`, TTL 3600s, purge UNREGISTERED).
- Angular : `push-notification.service.ts` (`isConfigured()`, `requestPermissionAndGetToken` + `registerToken`, `onMessage`), `notification.service.ts` (polling 30 s + SSE `startRealtime` reconnect 5 s + badge + `Notification` browser), `firebase-messaging-sw.js` (background + `tag`/`url` + click → focus/openWindow).
- Routage : `docs/notifications.md` (payment/order/dispute → rôles destinataires).

## Stratégie mobile : FCM d'abord, API toujours source de vérité

```
RabbitMQ event → notification-service → persist + SSE + FCM
  → Flutter : FCM (foreground/background/terminated) → refetch API → badge + deep-link
  → fallback sans FCM : polling 30 s unread-count + GET /notifications
```

1. FCM = déclencheur uniquement. À chaque push reçu : `GET /notifications?page=0&size=20` + `GET /unread-count`, dédup par `id`. Jamais d'affichage sur seul payload.
2. Enregistrement : au login `getToken()` → `POST /api/fcm-tokens`, `onTokenRefresh` → POST, au logout `DELETE` puis clear. Multi-device OK.
3. Foreground (`onMessage`) : notif locale (`flutter_local_notifications`, canal `payment_high`, tag = collapse, badge iOS) + invalidation `unreadCountProvider` + navigation si tap.
4. Background (`onBackgroundMessage` top-level `@pragma('vm:entry-point')`) : pas de refetch, juste notif système ; tap → `getInitialMessage` → deep-link.
5. SSE mobile : foreground uniquement, optionnel si FCM configuré ; obligatoire en fallback web-like avec reconnect 5 s. Jamais en background (batterie).
6. Permissions : demande après login (écran dédié + explication), état `granted/denied/unsupported` exposé, lien réglages OS si refusé.

## Mapping tag/url → route (implémenté dans `deep_link_mapper.dart`)

| tag / type | Route mobile | Exemple data.url backend |
|---|---|---|
| `payment.*` | `/payments/:id` | `/dashboard/payments/<id>` |
| `order.*` | supplier `/supplier/orders`, shop `/shop/orders/:id` | `/dashboard/supplier/orders`, `/dashboard/shop/orders/<id>` |
| `delivery.*` | `/supplier/deliveries` ou `/shop/orders/:id` | idem |
| `stock.low` | `/supplier/low-stock-alerts` | `/dashboard/supplier/low-stock-alerts` |
| `dispute.*` | `/shop/disputes/:id` | `/dashboard/shop/disputes/<id>` |
| défaut | `/notifications` | `/` |

Règle : strip `/dashboard` prefix, garder `:id`, query `ref` conservée. Si rôle non autorisé pour la cible → `/403` + toast.

## Écran `/notifications`

Liste paginée (20/page), filtre `type`, pull-to-refresh, mark-read optimiste (`POST /{id}/read`, rollback si échec), `Tout marquer lu` (`POST /read-all`), badge bottom-nav via `unread-count` (polling 30 s + push-triggered). Timestamps relatifs FR, icône par type (pas d'emoji).

## Backend à activer (checklist)

- [ ] `app.push.enabled=true` + service-account Firebase monté (notification-service).
- [ ] `google-services.json` (Android) + `GoogleService-Info.plist` (iOS) dans l'app mobile.
- [ ] Tester : login → token enregistré en base (`fcm_tokens`), paiement test → push reçue < 5 s, tap → bon écran, badge décrémenté après lecture.
- [ ] Sans config Firebase : vérifier fallback polling, aucun crash, log `push non configuré`.

## Tests

`flutter test test/notifications/` : mapping tag→route, conversion url Angular→mobile, register/unregister, dédup, mark-read optimiste, badge. Test manuel : kill app → push → tap → deep-link correct + `unread-count` cohérent.
