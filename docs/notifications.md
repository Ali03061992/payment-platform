# Notifications

## Architecture

```
Payment / Organization / Identity Services
        │  Domain Events (RabbitMQ)
        ▼
┌─────────────────────────────┐
│     Notification Service    │
│                             │
│  ┌───────────────┐  ┌───────┴────────┐
│  │ consume event │  │ SSE registry   │  (SseEmitter par userId)
│  │ → compute     │  │ push temps réel│
│  │ recipients    │  └───────┬────────┘
│  │ → persist     │          │
│  └───────┬───────┘          │
│          ▼                  ▼
│    notification_db      Angular (EventSource)
└─────────────────────────────┘
```

Le Notification Service est **totalement indépendant** du Payment Service : il ne connaît que les événements.

## Règles de routage (événement → destinataires)

| Événement | Destinataires |
|---|---|
| `PaymentCreatedEvent` | tous les **SUPPLIER_ADMIN + SUPPLIER_AGENT** du fournisseur ; **SHOP_ADMIN** de la boutique |
| `PaymentConfirmedEvent` | **SHOP_ADMIN + SHOP_AGENT** de la boutique ; **SUPPLIER_ADMIN** du fournisseur |
| `PaymentRejectedEvent` | **SHOP_ADMIN + SHOP_AGENT** de la boutique ; **SUPPLIER_ADMIN** du fournisseur |
| `PaymentCancelledEvent` | **SUPPLIER_ADMIN + SUPPLIER_AGENT** du fournisseur ; **SHOP_ADMIN** de la boutique |
| `SupplierDisabledEvent` | audit (log) |
| `UserDisabledEvent` | audit (log) |

> Le routage par rôle nécessite de connaître les utilisateurs d'une organisation. Deux options : (a) l'événement transporte les `recipientUserIds` calculés par l'émetteur ; (b) Notification interroge Identity (via Gateway) pour la liste des users d'une org. **Choix : (b)** — le Notification Service reste passif vis-à-vis du contenu et obtient la liste des destinataires au moment du traitement ; option (a) documentée comme optimisation.

## Modèle de données

`notifications(id, recipient_user_id, recipient_organization_id, type, message, read_status, created_at, read_at)` — voir database.md.

## Temps réel — SSE

- Endpoint : `GET /api/notifications/stream` (authentifié, `Authorization: Bearer`).
- Le service tient un registry `userId → SseEmitter` (avec timeout 30 s + heartbeat 15 s + reconnexion auto côté Angular).
- Au moment de la persistance, push de l'événement `notification.created` aux emitters de chaque destinataire connecté.
- Angular : `EventSource`-like via `HttpClient`/fetch avec token — implémentation `NotificationStreamService` (reconnexion exponentielle, filtre par destinataire côté serveur).

## API

| Méthode | Path | Description |
|---|---|---|
| GET | `/api/notifications?unreadOnly=&page=&size=` | mes notifications |
| PATCH | `/api/notifications/{id}/read` | marquer lue (propriétaire uniquement) |
| GET | `/api/notifications/unread-count` | compteur (polling léger) |
| GET | `/api/notifications/stream` | flux SSE |

Sécurité : un utilisateur ne voit/pousse que ses propres notifications (`recipient_user_id == jwt.sub`).

## Cas d'usage

- Agent boutique crée un paiement → le fournisseur voit « Nouveau paiement PAY-… de Boutique Tunis Centre » en temps réel.
- Agent fournisseur confirme → la boutique voit « Paiement PAY-… confirmé ».
- Refus → message avec motif de rejet.