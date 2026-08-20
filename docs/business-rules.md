# Règles métier

Statuts : utilisateur `ACTIVE | DISABLED`, organisation `ACTIVE | DISABLED`, paiement `PENDING | CONFIRMED | REJECTED | CANCELLED`.

## 1. Statut effectif d'un utilisateur

```
effectiveUserStatus = ACTIVE  ⟺  user.status = ACTIVE ∧ organization.status = ACTIVE
```

- Vérifié **côté backend à chaque requête** (jamais uniquement l'UI).
- Un utilisateur désactivé : login refusé, JWT refusé par le Gateway, et rejeté par chaque service (statut, org, rôle, ressource).

## 2. Désactivation d'une organisation (cascade obligatoire)

```
disableSupplier(id)  → org DISABLED → SupplierDisabledEvent
                        → Identity : tous SUPPLIER_ADMIN + SUPPLIER_AGENT de l'org → DISABLED
disableShop(id)      → org DISABLED → ShopDisabledEvent
                        → Identity : tous SHOP_ADMIN + SHOP_AGENT de l'org → DISABLED
```

Conséquences : plus de connexion, plus d'accès API, plus de création/confirmation de paiement, plus de notifications.

## 3. Réactivation

```
activateSupplier(id) / activateShop(id) / activateUser(id)
```

- Réactiver une organisation **ne réactive pas** les utilisateurs désactivés **individuellement** avant la désactivation de l'org (le bit individuel reste DISABLED).
- Tests dédiés : `disable supplier → disable user → disable supplier → activate supplier → user reste DISABLED`.

## 4. Désactivation individuelle

`disableUser(id)` n'affecte que l'utilisateur ciblé ; les autres agents restent ACTIVE.

## 5. Périmètre d'accès (multi-tenant)

| Acteur | Peut | Ne peut jamais |
|---|---|---|
| SYSTEM_ADMIN | tout (orgs, users, paiements, audits, stats) | — |
| SUPPLIER_ADMIN | gérer ses agents, voir ses boutiques liées, ses paiements, notifications | gérer un agent/boutique d'un autre fournisseur |
| SUPPLIER_AGENT | confirmer/refuser les paiements de son fournisseur, notifications | créer des paiements, toucher aux paiements d'un autre fournisseur |
| SHOP_ADMIN | gérer ses agents, ses paiements, notifications | agir hors de sa boutique |
| SHOP_AGENT | créer des paiements depuis sa boutique (vers fournisseur associé), voir ses paiements, notifications | confirmer/refuser |

Le filtrage par `organizationId` du JWT est appliqué dans les requêtes (jamais confiance aux IDs du body).

## 6. Paiement

- `amount > 0` ; `currency` obligatoire ; `shopId`, `supplierId`, `createdBy` obligatoires.
- À la création (vérifiées **synchrone** par Payment via Organization) :
  - utilisateur actif, boutique active, fournisseur actif,
  - **relation boutique ↔ fournisseur existante et active** (sinon 403/422).
- Statuts : `PENDING → CONFIRMED | REJECTED | CANCELLED` ; terminaux immuables ; `reject` exige `rejectionReason`.
- Concurrence : `@Version` (optimistic locking). Deux confirmations simultanées ⇒ une seule réussit, l'autre reçoit 409.
- Tout changement de statut est tracé dans `payment_events` (audit métier) + `audit_logs`.

## 7. Notifications

| Événement | Destinataires (rôles) |
|---|---|
| PaymentCreated | SUPPLIER_ADMIN + SUPPLIER_AGENT (du fournisseur), SHOP_ADMIN (de la boutique) |
| PaymentConfirmed | SHOP_ADMIN + SHOP_AGENT (boutique), SUPPLIER_ADMIN (fournisseur) |
| PaymentRejected | SHOP_ADMIN + SHOP_AGENT (boutique), SUPPLIER_ADMIN (fournisseur) |
| PaymentCancelled | SUPPLIER_ADMIN + SUPPLIER_AGENT, SHOP_ADMIN |
| SupplierDisabled | (audit) |
| UserDisabled | (audit) |

Une notification est persistée (lue/non lue) et poussée en temps réel (SSE) aux utilisateurs connectés.

## 8. Audits

Actions tracées : `PAYMENT_CREATED|PAYMENT_CONFIRMED|PAYMENT_REJECTED|PAYMENT_CANCELLED`, `SUPPLIER_CREATED|SUPPLIER_ENABLED|SUPPLIER_DISABLED`, `SHOP_CREATED|SHOP_ENABLED|SHOP_DISABLED`, `USER_CREATED|USER_ENABLED|USER_DISABLED`.

Chaque entrée : `userId, organizationId, action, entityId, timestamp, details(JSON)`.