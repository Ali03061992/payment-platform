# Bounded Contexts

## Carte des contextes

```
┌─────────────────────────────────────────────────────────────────┐
│                      Payment Platform                           │
│                                                                 │
│  ┌──────────────────┐    ┌──────────────────────────────┐       │
│  │  IdentityContext │    │    OrganizationContext       │       │
│  │  (Identity Svc)  │    │    (Organization Service)    │       │
│  │                  │    │                              │       │
│  │ User, Role,      │◄───│ Supplier, Shop,              │       │
│  │ Session, JWT     │    │ SupplierShopRelation         │       │
│  │                  │    │                              │       │
│  └──────────────────┘    └──────────────────────────────┘       │
│           ▲                              ▲                       │
│           │ events                       │ events                │
│           │                              │                       │
│  ┌────────┴────────────────────────────────┴──────────────┐     │
│  │                   RabbitMQ (broker)                     │     │
│  └────────┬───────────────────────────────┬───────────────┘     │
│           │ events                        │ events              │
│           ▼                               ▼                      │
│  ┌──────────────────┐    ┌──────────────────────────────┐       │
│  │  PaymentContext  │    │    NotificationContext       │       │
│  │  (Payment Svc)   │    │    (Notification Service)    │       │
│  │                  │    │                              │       │
│  │ Payment, Money,  │───▶│ Notification, ReadStatus     │       │
│  │ PaymentReference │    │                              │       │
│  └──────────────────┘    └──────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## Frontières

| Contexte | Propriétaire (Aggregate) | Ce qu'il NE possède PAS |
|---|---|---|
| Identity | User (credentials, statut) | organisations, relations, paiements |
| Organization | Organization, SupplierShopRelation | comptes utilisateurs, paiements |
| Payment | Payment | statut des comptes, organisation (vérifiées à distance) |
| Notification | Notification | cycle de vie des paiements/orgs (il n'en est que consommateur) |

## Langage ubiquitaire

| Terme | Définition |
|---|---|
| Paiement unitaire | Transfert d'un montant entre une boutique (débiteur) et un fournisseur (créancier) |
| Fournisseur | Organization de type SUPPLIER |
| Boutique | Organization de type SHOP |
| Relation | Association validée boutique ↔ fournisseur autorisant les paiements |
| Confirmation | Action d'un agent fournisseur validant un paiement PENDING |
| Refus | Action d'un agent fournisseur rejetant un paiement PENDING (motif obligatoire) |
| Désactivation | Passage d'une org/utilisateur à l'état DISABLED, bloquant toute action |

## Anti-corruption

- **Payment ne voit jamais le modèle User/Organization** : il manipule des `OrganizationId`/`UserId` typés et interroge l'état (actif ? relation ?) par appel REST via le Gateway au moment de la création.
- **Identity ne reçoit que des événements de désactivation** (`SupplierDisabledEvent`, `ShopDisabledEvent`) et applique la cascade localement (transaction locale, idempotente).
- **Notification ne connaît que des identifiants et rôles** : il route les notifications par `recipientUserId` / `recipientOrganizationId` portés par les événements.
- Les événements sont des **contrats** versionnés (champ `eventVersion`) partagés via le module `shared-lib`.

## Carte des événements par contexte

| Émetteur | Événements publiés | Consommateurs |
|---|---|---|
| Identity | `UserCreatedEvent`, `UserActivatedEvent`, `UserDisabledEvent` | Notification |
| Organization | `SupplierCreatedEvent`, `SupplierActivatedEvent`, `SupplierDisabledEvent`, `ShopCreatedEvent`, `ShopActivatedEvent`, `ShopDisabledEvent` | Identity, Notification |
| Payment | `PaymentCreatedEvent`, `PaymentConfirmedEvent`, `PaymentRejectedEvent`, `PaymentCancelledEvent` | Notification |