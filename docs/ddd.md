# DDD — Domain Model

> Canonique (court, à jour). Compléments : [bounded-contexts.md](bounded-contexts.md) (résumé),
> [business-rules.md](business-rules.md) (règles), [domain-model-complete.md](domain-model-complete.md)
> (référence exhaustive). Historiques : [DDD_IMPROVEMENT_PLAN.md](DDD_IMPROVEMENT_PLAN.md),
> [ANALYSE_FONCTIONNELLE_DDD.md](ANALYSE_FONCTIONNELLE_DDD.md). Index : [README.md](README.md).

Chaque microservice suit une **architecture hexagonale** :

```
service/
├── domain/            # 100% métier, sans dépendance Spring ni infrastructure
│   ├── model/         # aggregates & entities
│   ├── valueobject/   # value objects
│   ├── event/         # domain events
│   ├── repository/    # interfaces de persistance (ports)
│   └── service/       # domain services
├── application/       # use cases (orchestration), commands/queries, DTOs
│   ├── command/
│   ├── query/
│   ├── usecase/
│   └── dto/
├── infrastructure/    # adapters : JPA, RabbitMQ, configuration
│   ├── persistence/
│   ├── messaging/
│   └── configuration/
└── interfaces/
    └── rest/          # controllers REST, OpenAPI
```

**Règle de dépendance** : `interfaces → application → domain` ; `infrastructure → domain` (les dépendances pointent vers l'intérieur). Le domaine ne connaît ni Spring, ni JPA, ni RabbitMQ.

## Identity Context

### Aggregate Root : `User`
- `id: UserId`, `username: Username`, `email: Email`, `password: PasswordHash`,
  `firstName`, `lastName`, `phone: PhoneNumber`, `organizationId: OrganizationId`,
  `status: UserStatus`, `version` (optimistic lock), `createdAt`, `updatedAt`
- Invariants : username/email uniques ; status ∈ {ACTIVE, DISABLED} ; rôle ∈ {SYSTEM_ADMIN, SUPPLIER_ADMIN, SUPPLIER_AGENT, SHOP_ADMIN, SHOP_AGENT}
- Comportement : `disable()`, `activate()`, `changeRole()`, `changeOrganization()`, `updateProfile()`, `verifyPassword()`
- Un `SYSTEM_ADMIN` n'est rattaché à aucune organisation (`organizationId = null`)

### Entity : `Role`
Code, nom. Définit les permissions : voir [security.md](security.md).

## Organization Context

### Aggregate Root : `Organization`
- `id: OrganizationId`, `name: OrganizationName`, `type: OrganizationType` (SUPPLIER|SHOP),
  `status: OrganizationStatus` (ACTIVE|DISABLED), `version`, `createdAt`, `updatedAt`
- Comportement : `disable()` (invariant : désactivation idempotente), `activate()`
- Invariant : la désactivation d'une organisation **ne réactive jamais** les comptes utilisateurs désactivés individuellement (bit individuel conservé dans Identity)

### Entity : `SupplierShopRelation`
- `id`, `supplierId`, `shopId`, `status`, `createdAt`
- Unique par couple (supplierId, shopId) ; permet N:N boutiques ↔ fournisseurs

> Supplier et Shop sont des **vues** d'une Organization (type SUPPLIER / type SHOP). Il n'existe pas de table séparée ; les relations référencent `organization_id`.

## Payment Context

### Aggregate Root : `Payment`
- `id: PaymentId`, `reference: PaymentReference`, `shopId: OrganizationId`, `supplierId: OrganizationId`,
  `money: Money`, `status: PaymentStatus`, `rejectionReason: RejectionReason?`,
  `createdBy: UserId`, `version` (optimistic lock), `createdAt`, `updatedAt`
- Machine à états (immuable après transition terminale) :

```
PENDING ──confirm()──▶ CONFIRMED
PENDING ──reject(r)──▶ REJECTED
PENDING ──cancel()──▶ CANCELLED
```

- Invariants : `amount > 0` ; `confirm()` exige PENDING ; `reject(reason)` exige PENDING et reason non vide ; `cancel()` exige PENDING ; les statuts terminaux (CONFIRMED/REJECTED/CANCELLED) sont immuables
- Concurrence : `version` (Optimistic Locking) — une seule confirmation simultanée réussit

### Entity : `PaymentEvent` (historique/audit métier)
`id, paymentId, action (PAYMENT_CREATED|PAYMENT_CONFIRMED|PAYMENT_REJECTED|PAYMENT_CANCELLED), userId, timestamp, details`

## Notification Context

### Aggregate Root : `Notification`
- `id`, `recipientUserId: UserId`, `recipientOrganizationId: OrganizationId`,
  `type: NotificationType`, `message`, `readStatus: ReadStatus`, `createdAt`, `readAt`
- Comportement : `markAsRead()` (idempotent)

## Value Objects (récapitulatif)

| VO | Contexte | Contraintes |
|---|---|---|
| `UserId`, `OrganizationId` | tous | identifiants typés |
| `Username` | Identity | non vide, 3–50 caractères |
| `Email` | Identity | format RFC 5322 |
| `PasswordHash` | Identity | BCrypt, ≥ 60 chars |
| `PhoneNumber` | Identity | optionnel, format E.164 |
| `OrganizationName` | Organization | non vide, ≤ 100 |
| `Money(amount, currency)` | Payment | `amount > 0`, 2–4 décimales |
| `PaymentReference` | Payment | généré : `PAY-<timestamp>-<aléa>` unique |
| `RejectionReason` | Payment | non vide, ≤ 500 |