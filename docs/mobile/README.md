# Mobile — README

Socle mobile Flutter Payment Platform. Cible : **SUPPLIER_ADMIN, SUPPLIER_AGENT, SHOP_ADMIN, SHOP_AGENT**. SYSTEM_ADMIN exclu (reste Angular).

## Docs

| Doc | Contenu |
|-----|---------|
| `01-architecture.md` | Choix Flutter, structure, auth, router, offline, sécurité |
| `02-parity-matrix.md` | **Référence** : chaque route Angular → écran mobile, par rôle |
| `03-notifications.md` | FCM + SSE + polling, mapping tag→route, deep-links — **priorité** |
| `04-setup.md` | Install, Firebase, env, build Android/iOS, tests |

## Agents

- `.opencode/agents/flutter.md` — dev Flutter général, parité front, Riverpod/Dio/go_router.
- `.opencode/agents/mobile-notifications.md` — push temps réel, FCM/SSE, deep-links. **À invoquer en premier sur tout sujet notif.**

## App

- Code : `payment-platform-mobile/`
- API via gateway `http://localhost:8081` (prod : variable `API_BASE_URL`), mêmes DTO que `payment-platform-ui/src/app/services/*.ts`.
- Source de vérité routes : `FRONTEND_ROUTES.md` + `payment-platform-ui/src/app/app-routing.module.ts`.
