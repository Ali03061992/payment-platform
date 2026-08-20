# Événements & Cohérence distribuée

## Principe

Aucune transaction SQL distribuée. Chaque écriture critique est faite dans une **transaction locale** couvrant l'agrégat **et** sa ligne `outbox_events` :

```
┌─────────────────────── transaction locale ───────────────────────┐
│  payment_db                                                       │
│    ├── INSERT payments (status = PENDING)                         │
│    └── INSERT outbox_events (PaymentCreatedEvent, payload JSON)   │
└───────────────────────────────────────────────────────────────────┘
                        │
                        ▼  (relais @Scheduled, toutes les 1 s)
              Event Publisher ──▶ RabbitMQ (exchange `payment.events`)
                        │
                        ▼
              Notification Service (consommateur idempotent)
```

⇒ **Un paiement n'existe jamais sans son événement** (et inversement).

## Outbox

- Table `outbox_events` par service émetteur (voir database.md).
- Relais : `@Scheduled` sélectionne `processed_at IS NULL ORDER BY id LIMIT 100`, publie sur RabbitMQ, puis marque `processed_at` (idempotence par `event_id` unique sur le consommateur).
- Échec de publication → retry au tick suivant ; alertes loggées au-delà de N tentatives.
- **Événements critiques sous outbox** : tous les `Payment*Event`, `SupplierDisabledEvent`, `ShopDisabledEvent`, `UserDisabledEvent`.
- Les événements non critiques (ex. `SupplierCreatedEvent` informatif) peuvent être publiés directement, mais **tous** sont traités de façon idempotente par les consommateurs.

## Événements (contrat partagé, module `shared-lib`)

Chaque événement porte : `eventId (UUID)`, `eventType`, `eventVersion`, `occurredAt`, `aggregateId`, payload typé.

| Événement | Payload |
|---|---|
| `PaymentCreatedEvent` | paymentId, reference, shopId, supplierId, amount, currency, createdBy, occurredAt |
| `PaymentConfirmedEvent` | paymentId, supplierId, shopId, confirmedBy, occurredAt |
| `PaymentRejectedEvent` | paymentId, supplierId, shopId, rejectedBy, rejectionReason, occurredAt |
| `PaymentCancelledEvent` | paymentId, supplierId, shopId, cancelledBy, occurredAt |
| `SupplierCreatedEvent` | organizationId, name |
| `SupplierActivatedEvent` / `SupplierDisabledEvent` | organizationId |
| `ShopCreatedEvent` / `ShopActivatedEvent` / `ShopDisabledEvent` | organizationId |
| `UserCreatedEvent` / `UserActivatedEvent` / `UserDisabledEvent` | userId, organizationId, roles |

## Topologie RabbitMQ

```
exchange amq.topic
  payment.events    → queue notification.payments   (Payment*Event)
  organization.events → queue identity.org-status    (Supplier/Shop Disabled/Activated)
                     → queue notification.orgs       (tous les événements org)
  identity.events   → queue notification.users      (User*Event)
```

Routing keys par type d'événement (ex. `payment.created`, `organization.supplier.disabled`).

## Idempotence des consommateurs

- Table de dédup côté consommateur : `event_id` unique (colonne dans une table `processed_events` ou `UNIQUE` sur `outbox_events` côté émetteur pour le rejeu).
- La cascade de désactivation est **répétable** : `disable` d'un utilisateur déjà DISABLED ne change rien (idempotent) et re-publie l'audit sans dupliquer.

## Cascade de désactivation (flux complet)

```
SYSTEM_ADMIN ──PATCH /admin/suppliers/1/disable──▶ Organization
  tx locale : organizations.status=DISABLED + audit + outbox(SupplierDisabledEvent)
  ──▶ RabbitMQ ──▶ Identity
      tx locale : UPDATE users SET status=DISABLED WHERE organization_id=1 AND role IN (SUPPLIER_ADMIN,SUPPLIER_AGENT)
                  + audit (USER_DISABLED × n) + outbox(UserDisabledEvent × n)
      ──▶ Notification : notifications de désactivation (audit)
```

## Compensation & reprise

- Rejeu des événements : `event_id` idempotent.
- Scénario de crash entre « paiement créé » et « événement publié » : l'outbox garantit la publication au prochain tick.
- Scénario de double publication (crash après envoi avant `processed_at`) : le consommateur déduplique via `event_id`.