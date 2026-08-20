# Stratégie de tests

## Pyramide

```
        E2E (Playwright)          — parcours multi-rôles, docker compose complet
      Front (Jest/Testing lib)    — composants, services, guards, interceptors
    Integration (Testcontainers)  — MySQL 8.4 + RabbitMQ réels, JPA, outbox, SSE, sécurité
  Unit (JUnit5/Mockito/AssertJ)   — domain services, use cases, VOs, machine à états
```

**Règle** : jamais de H2 pour simuler MySQL (divergences de SQL, verrous, DECIMAL). Les tests d'intégration utilisent **Testcontainers** (`mysql:8.4`, `rabbitmq:4-management`) via le module Spring Boot `@ServiceConnection`.

## Unit tests (domaine pur)

- Identity : `User` (disable/activate, invariants), PasswordHash, règles de cascade.
- Organization : `Organization.disable/activate`, relation unique, idempotence.
- Payment : machine à états (PENDING→CONFIRMED/REJECTED/CANCELLED, transitions interdites), `Money` (amount>0, devise), `PaymentReference`.
- Notification : `markAsRead` idempotent, routage destinataires.

## Integration tests (Testcontainers)

| Service | Scénarios |
|---|---|
| Identity | login OK / désactivé / org désactivée ; JWT rejeté après désactivation ; cascade via `SupplierDisabledEvent`/`ShopDisabledEvent` (RabbitMQ réel) ; « user désactivé individuellement reste désactivé après réactivation de l'org » |
| Organization | CRUD orgs ; relation unique ; désactivation → événement publié via outbox ; stats agents/boutiques |
| Payment | création (validation relation via Organization HTTP mock ou réel) ; confirm/reject/cancel ; **concurrence : 2 confirmations simultanées → 1 succès, 1 × 409** ; outbox → `PaymentCreatedEvent` publié sur RabbitMQ et consommé par Notification |
| Notification | consommation `PaymentCreatedEvent` → persistance + SSE push ; dédup par `event_id` |
| Gateway | routing, CORS, rate-limit, correlation-id (WebTestClient) |

## Security tests

| # | Scénario (spec §32) | Attendu |
|---|---|---|
| 1 | SYSTEM_ADMIN crée un fournisseur | 201 |
| 2 | SYSTEM_ADMIN désactive un fournisseur | 200 + cascade |
| 3 | SUPPLIER_ADMIN crée un agent de SON fournisseur | 201 |
| 4 | SUPPLIER_ADMIN tente de gérer un agent d'UN AUTRE fournisseur | 403 |
| 5 | SHOP_AGENT crée un paiement | 201 |
| 6 | SHOP_AGENT tente de confirmer un paiement | 403 |
| 7 | SUPPLIER_AGENT confirme un paiement | 200 |
| 8 | utilisateur désactivé se connecte | 401/403 |
| 9 | agent du fournisseur A accède aux paiements du fournisseur B | 403/404 |
| 10 | paiement vers un fournisseur non associé à la boutique | 422 |

## Tests de désactivation (explicites)

- `disableSupplier()` → tous les utilisateurs fournisseur DISABLED (vérification en base).
- `disableShop()` → tous les utilisateurs boutique DISABLED.
- `disableUser()` individuel → les autres restent ACTIVE.
- `disable supplier → disable user → activate supplier → user reste DISABLED`.

## Contract tests (événements)

- Test de sérialisation des événements (`eventType`, `eventVersion`, payload) entre `shared-lib` et chaque consommateur.
- Test de bout en bout : outbox → RabbitMQ → consommateur (dédup).

## Frontend (Jest)

- Guards de routes par rôle ; interceptor JWT (ajout token, 401 → logout) ; services (auth, payments, notifications SSE) ; composants clés (formulaire paiement, liste, notification).

## E2E (Playwright)

- `docker compose up` complet, puis : login SYSTEM_ADMIN → créer fournisseur → créer boutique → lier → login shop.agent → créer paiement → login supplier.agent → confirmer → vérifier notification temps réel.

## Exécution

```bash
./mvnw test                       # unit + intégration (Testcontainers nécessite Docker)
npm test                          # frontend (Jest)
npx playwright test               # e2e
```

CI (GitHub Actions) : `compile → unit → integration → security → frontend → e2e → docker build`. Échec ⇒ pipeline rouge.