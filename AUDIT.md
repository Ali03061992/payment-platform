# Audit Complet de l'Application Payment Platform

## 1. Vue d'ensemble

### Architecture
Plateforme de paiement B2B multi-services microservices reliant fournisseurs, boutiques et livreurs.

```
┌──────────────┐     ┌──────────────┐
│  Angular UI  │────▶│  API Gateway │ (port 8081)
│  (port 4200) │     │  Routing/JWT │
└──────────────┘     └──────┬───────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼──────┐  ┌────────▼───────┐  ┌───────▼──────┐
│   Identity   │  │  Organization  │  │   Payment    │
│  (port 8082) │  │  (port 8083)   │  │  (port 8084) │
│  Auth/Users  │  │  Orders/Stock  │  │  Payments    │
└───────┬──────┘  └────────┬───────┘  └───────┬──────┘
        │                  │                   │
        └──────────┬───────┴───────────────────┘
                   │
            ┌──────▼───────┐
            │ Notification │
            │ (port 8085)  │
            │ SSE + AMQP   │
            └──────────────┘
```

### Stack technique
| Couche | Technologie |
|--------|------------|
| Backend | Java 26, Spring Boot 4.1.0, Spring Cloud 2025.1.2 |
| Frontend | Angular (JIT), TypeScript, RxJS |
| BDD | MySQL 8 (prod), H2 (test) |
| Messaging | RabbitMQ (AMQP) |
| Auth | JWT (OAuth2 Resource Server) |
| API Docs | Springdoc OpenAPI 3.0 |
| Tests | JUnit 5 + H2 (backend), Karma (frontend), Cypress (E2E) |
| Build | Maven 3.9, npm |
| Container | Docker multi-stage |

### Rôles utilisateurs
| Rôle | Description |
|------|------------|
| SYSTEM_ADMIN | Admin global |
| SUPPLIER_ADMIN | Admin fournisseur (gestion commandes, produits, stock) |
| SUPPLIER_AGENT | Livreur (livraisons assignées) |
| SHOP_ADMIN | Admin boutique (commandes, paiements) |
| SHOP_MANAGER | Manager boutique (commandes, paiements) |
| SHOP_AGENT | Agent boutique (consultation) |

---

## 2. Backend - Organization Service

### 2.1 Modèle de données

**Entités principales:**
- `Organization` - Fournisseurs et boutiques (UUID, name, type, status)
- `Order` - Commandes avec cycle de vie complet (14 statuts)
- `OrderItem` - Articles d'une commande
- `OrderEvent` - Historique des événements par commande
- `Product` - Catalogue produits
- `ProductFamily` / `ProductCategory` - Classifications
- `StockMovement` - Mouvements de stock
- `BalanceEntry` - Grand livre des balances
- `SupplierShopRelation` - Relations fournisseur-boutique

**Statuts de commande:**
```
DRAFT → CONFIRMED → PREPARING → READY_FOR_DELIVERY
                                              ↓
                                    (assign agent)
                                              ↓
                                     DELIVERY_ACCEPTED → IN_DELIVERY → DELIVERED → ACCEPTED
                                              ↑                  ↑
                                    (agent reject)      (supplier admin reject)
                                              ↓                  ↓
                                     DELIVERY_REJECTED    DELIVERY_REJECTED

DRAFT/CONFIRMED/PREPARING → CANCELLED
DELIVERED → REJECTED
```

### 2.2 Endpoints API

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/api/orders` | SUPPLIER_ADMIN, SHOP_ADMIN/MANAGER | Créer commande |
| GET | `/api/orders` | Tous | Lister commandes (filtré par rôle) |
| GET | `/api/orders/{id}` | Tous | Détail commande |
| GET | `/api/orders/reference/{ref}` | Tous | Détail par référence |
| POST | `/api/orders/{id}/confirm` | SUPPLIER_ADMIN | Confirmer |
| POST | `/api/orders/{id}/prepare` | SUPPLIER_ADMIN | Préparer |
| POST | `/api/orders/{id}/ready` | SUPPLIER_ADMIN | Prêt livraison |
| POST | `/api/orders/{id}/assign-delivery` | SUPPLIER_ADMIN | Assigner livreur |
| POST | `/api/orders/{id}/accept-delivery` | SUPPLIER_AGENT | Accepter/rejeter |
| POST | `/api/orders/{id}/confirm-delivery` | SUPPLIER_AGENT | Confirmer date |
| POST | `/api/orders/{id}/deliver` | SUPPLIER_AGENT/ADMIN | Livrer |
| POST | `/api/orders/{id}/accept` | SHOP_ADMIN/MANAGER | Accepter |
| POST | `/api/orders/{id}/accept-asap` | SHOP_ADMIN/MANAGER | Accepter + paiement ASAP |
| POST | `/api/orders/{id}/cancel` | SUPPLIER_ADMIN, SHOP_ADMIN/MANAGER | Annuler |
| POST | `/api/orders/{id}/reject` | SHOP_MANAGER/ADMIN | Rejeter |
| POST | `/api/orders/{id}/delivery-reject` | SUPPLIER_ADMIN/AGENT | Rejeter livraison |
| GET | `/api/orders/deliveries` | Tous rôles autorisés | Livraisons |
| GET | `/api/orders/my-deliveries` | SUPPLIER_AGENT | Mes livraisons |
| GET | `/api/orders/shop-agents` | Tous rôles autorisés | Agents boutique |

**Autres controllers:**
- `AdminOrganizationController` - CRUD organisations (SYSTEM_ADMIN)
- `BalanceController` - Grand livre des balances
- `CatalogController` - Catalogue produits
- `StockController` - Mouvements de stock
- `StockOptimizationController` - Moteur d'optimisation ABC/XYZ/EOQ
- `SupplierShopRelationController` - Relations fournisseur-boutique
- `InternalOrganizationController` - API inter-services

### 2.3 Use Cases (15)

| Use Case | Description |
|----------|-------------|
| `CreateOrderUseCase` | Création avec calcul totaux, génération référence |
| `ConfirmOrderUseCase` | DRAFT → CONFIRMED |
| `PrepareOrderUseCase` | CONFIRMED → PREPARING → READY_FOR_DELIVERY |
| `AcceptDeliveryUseCase` | Agent accepte/rejette la livraison |
| `ConfirmDeliveryUseCase` | Agent confirme date de livraison |
| `DeliverOrderUseCase` | Marquer livré + création paiement ASAP automatique |
| `DeliveryRejectOrderUseCase` | Admin rejette livraison avec motif |
| `AcceptOrderUseCase` | Boutique accepte livraison |
| `CancelOrderUseCase` | Annulation |
| `RejectOrderUseCase` | Rejet par la boutique |
| `CreateOrganizationUseCase` | Création fournisseur/boutique |
| `OrganizationQueryUseCase` | Requêtes organisations |
| `OrganizationStatusUseCase` | Activation/désactivation |
| `SupplierShopRelationUseCase` | Relations F&B |
| `BalanceUseCase` | Grand livre |

### 2.4 Infrastructure

- **IdentityClient** - HTTP client pour résoudre noms via identity-service
- **PaymentClient** - HTTP client pour créer paiements ASAP automatiques
- **OutboxEventStore** - Pattern outbox pour événements fiables
- **AmqpTopology** - Topology RabbitMQ (exchanges + queues)
- **LowStockAlertScheduler** - Scheduler alertes stock bas
- **Moteurs d'optimisation** - ABCClassifier, XYZClassifier, EOQCalculator, SafetyStockCalculator, DemandForecaster, AnomalyDetector

---

## 3. Backend - Identity Service

### 3.1 Modèle de données
- `User` - Utilisateurs (UUID, email, firstName, lastName, phoneNumber, status)
- `UserStatus` - ENABLED / DISABLED
- Value objects: `Email`, `PasswordHash`, `PhoneNumber`, `Username`
- Tables: `users`, `roles`, `user_roles`, `permissions`, `role_permissions`, `password_setup_tokens`

### 3.2 Endpoints
| Endpoint | Description |
|----------|-------------|
| POST `/api/auth/login` | Connexion JWT |
| POST `/api/auth/register` | Auto-inscription |
| GET `/api/auth/me` | Profil utilisateur connecté |
| POST `/api/auth/change-password` | Changement mot de passe |
| POST `/api/auth/password-setup/*` | Setup mot de passe par token |
| GET `/api/users` | Liste utilisateurs |
| GET `/api/users/{id}` | Détail utilisateur |
| PATCH `/api/users/{id}/activate` | Activer |
| PATCH `/api/users/{id}/disable` | Désactiver |
| GET `/api/suppliers/{id}/agents` | Agents d'un fournisseur |
| POST `/api/suppliers/{id}/agents` | Créer agent |
| PATCH `/api/suppliers/{id}/agents/{agentId}` | Modifier agent |
| POST `/api/internal/users` | Création inter-services |

### 3.3 Use Cases (8)
`AuthUseCase`, `RegisterUseCase`, `UserQueryUseCase`, `UserStatusUseCase`, `AgentManagementUseCase`, `PasswordSetupUseCase`, `ChangePasswordUseCase`, `InternalUserCreationUseCase`, `OrganizationCascadeUseCase`

---

## 4. Backend - Payment Service

### 4.1 Modèle de données
- `Payment` - Paiements (UUID, reference, amount, currency, status, shopId, supplierId)
- `PaymentStatus` - PENDING / CONFIRMED / REJECTED / CANCELLED
- `PaymentEvent` - Événements par paiement

### 4.2 Endpoints
| Endpoint | Description |
|----------|-------------|
| POST `/api/payments` | Créer paiement |
| GET `/api/payments` | Lister (SYSTEM_ADMIN / paginé) |
| GET `/api/payments/{id}` | Détail |
| GET `/api/payments/reference/{ref}` | Par référence |
| POST `/api/payments/{id}/confirm` | Confirmer |
| POST `/api/payments/{id}/reject` | Rejeter (avec motif) |
| POST `/api/payments/{id}/cancel` | Annuler |
| GET `/api/payments/stats` | Statistiques |
| GET `/api/payments/agent-summary` | Résumé agent |
| GET `/api/payments/export/csv` | Export CSV |

### 4.3 Use Cases (7)
`CreatePaymentUseCase`, `ConfirmPaymentUseCase`, `RejectPaymentUseCase`, `CancelPaymentUseCase`, `GetPaymentUseCase`, `ListPaymentsUseCase`, `AgentPaymentsBySupplierUseCase`

---

## 5. Backend - Notification Service

- Consumes events AMQP (payments + orders)
- Broadcaster SSE pour notifications temps réel
- Heartbeat scheduler pour keepalive
- Stockage notifications en BDD

---

## 6. Frontend - Angular

### 6.1 Composants (30)

**Layout:**
- `LayoutComponent` - Sidebar + header + navigation + notifications

**Dashboard:**
- `DashboardComponent` - Vue d'ensemble avec stats

**Fournisseur (Supplier):**
- `OrderManagementComponent` - Gestion commandes + livraisons (2 onglets)
- `DeliveryManagementComponent` - Mes livraisons (agent)
- `StockOptimizationComponent` - Optimisation stock ABC/XYZ

**Boutique (Shop):**
- `OrderListComponent` - Liste commandes boutique
- `OrderDetailComponent` - Détail commande boutique
- `OrderCreateComponent` - Création commande

**Paiements:**
- `PaymentListComponent` - Liste paiements
- `PaymentDetailComponent` - Détail paiement + QR code
- `PaymentCreateComponent` - Création paiement
- `PaymentStatsComponent` - Statistiques paiements

**Admin:**
- `SupplierListComponent` - Liste fournisseurs
- `ShopListComponent` - Liste boutiques
- `UserListComponent` - Liste utilisateurs
- `UserCreateComponent` - Création utilisateur
- `SupplierCreateComponent` - Création fournisseur
- `ShopCreateComponent` - Création boutique
- `AgentCreateComponent` - Création agent
- `RelationCreateComponent` - Création relation F&B

### 6.2 Services (13)
`AuthService`, `OrderService`, `PaymentService`, `SupplierService`, `ShopService`, `UserService`, `SupplierAgentService`, `NotificationService`, `DashboardService`, `StockService`, `StockOptimizationService`, `BalanceService`, `RelationService`

### 6.3 Routing
```
/dashboard                           → DashboardComponent
/dashboard/supplier/orders           → OrderManagementComponent
/dashboard/supplier/deliveries       → DeliveryManagementComponent
/dashboard/supplier/stock-optimize   → StockOptimizationComponent
/dashboard/shop/orders               → OrderListComponent
/dashboard/shop/orders/create        → OrderCreateComponent
/dashboard/shop/orders/:id           → OrderDetailComponent
/dashboard/payments                  → PaymentListComponent
/dashboard/payments/create           → PaymentCreateComponent
/dashboard/payments/stats            → PaymentStatsComponent
/dashboard/payments/:id              → PaymentDetailComponent
/dashboard/admin/suppliers           → SupplierListComponent
/dashboard/admin/shops               → ShopListComponent
/dashboard/admin/users               → UserListComponent
/dashboard/admin/users/create        → UserCreateComponent
/dashboard/admin/suppliers/create    → SupplierCreateComponent
/dashboard/admin/shops/create        → ShopCreateComponent
/dashboard/admin/agents/create       → AgentCreateComponent
/dashboard/admin/relations/create    → RelationCreateComponent
/login                               → LoginComponent
/register                            → RegisterComponent
```

### 6.4 Tests

**Karma (803 tests):** Tous composants, services, pipes, guards testés.

**Cypress E2E (14 suites):**
| Suite | Tests |
|-------|-------|
| `01-login` | Authentification |
| `02-admin-organizations` | CRUD organisations |
| `03-admin-users` | Gestion utilisateurs |
| `04-supplier-products` | Produits |
| `05-supplier-stock` | Stock |
| `06-supplier-agents` | Agents |
| `07-supplier-orders` | Commandes fournisseur |
| `08-shop-orders` | Commandes boutique |
| `09-payments` | Paiements |
| `10-relations` | Relations F&B |
| `11-dashboard` | Dashboard |
| `12-balance` | Balance |
| `13-stock-optimization` | Optimisation stock |
| `14-delivery-workflow` | Cycle de vie livraison complet |

---

## 7. Bugs et Problèmes Identifiés

### Critique (P0)

| # | Fichier | Problème |
|---|---------|----------|
| 1 | `PaymentClient.java:30` | `createdBy` param accepté mais jamais envoyé dans le body de la requête POST paiement |
| 2 | `OrderController.java:132` | `deliveryRejectOrder` Accepte body nullable mais `body.get("reason")` peut NPE si body non null mais clé absente |

### Élevé (P1)

| # | Fichier | Problème |
|---|---------|----------|
| 3 | `IdentityClient.java` | Timeout non configuré sur les appels HTTP - peut bloquer les threads si identity-service down |
| 4 | `PaymentClient.java` | Même problème - timeout HTTP non configuré |
| 5 | `OrderController.java:84-91` | `buildOrderResponse()` fait 2 appels HTTP (resolveUserName) par commande - N+1 queries potentiel pour les listes |
| 6 | `DeliverOrderUseCase.java:60-67` | Création paiement ASAP dans le même transaction que la livraison - si paiement échoue, la livraison est quand même commitée |
| 7 | `layout.component.ts` | `navigateNotification()` ne vérifie pas si l'entité existe avant de naviguer - peut afficher page erreur |
| 8 | `payment-detail.component.ts:35` | UUID regex trop strict - rejette les UUIDs avec majuscules si mal formatés |

### Moyen (P2)

| # | Fichier | Problème |
|---|---------|----------|
| 9 | `order-management.component.ts` | `loadOrders()` et `loadDeliveries()` font des appels API séparés - pourrait être combiné |
| 10 | `delivery-management.component.ts` | `openDeliver()` fait un appel API pour shopAgents à chaque ouverture - pas de cache |
| 11 | `PaymentService.java` (frontend) | `list()` gère 4 formes de réponse différents (array, items, content, data) - code defensif excessif |
| 12 | `OrderService.java` (frontend) | Même problème - gestion de 4 shapes différents |
| 13 | `order-detail.component.ts` (shop) | Pas de gestion d'erreur quand l'id n'est pas trouvé |
| 14 | `StockOptimizationComponent` | affiche des données mockées/pas de vrai backend pour certaines métriques |
| 15 | `BalanceComponent` | Pas de pagination côté frontend |

### Bas (P3)

| # | Fichier | Problème |
|---|---------|----------|
| 16 | `layout.component.css` | Scrollbar customisée - problème d'accessibilité |
| 17 | Tous les composants | `@ts-nocheck` dans les specs - TypeScript non vérifié dans les tests |
| 18 | `order-management.component.html` | Modals avec `(click)="closeDetail()"` sur overlay - pas de focus trap |
| 19 | `payment-detail.component.html` | QR Code data hardcodé - pas de lien dynamic basé sur l'URL réelle |
| 20 | Cypress tests | Certains tests dépendent de données seed - pas 100% isolés |

---

## 8. Améliorations Proposées

### P0 - Immédiat

1. **OrderController deliveryReject** - Ajouter null check sur body avant d'accéder aux clés
2. **Performance listes** - Agréger les résolutions de noms dans une seule requête ou utiliser un batch resolver

### P1 - Court terme

3. **Timeout HTTP** - Configurer timeouts sur IdentityClient et PaymentClient (5s connect, 10s read)
4. **Paiement ASAP transaction** - Déplacer la création ASAP dans un @Async ou un event listener séparé
5. **Cache shop agents** - Ajouter un cache simple (TTL 5min) pour les appels getShopAgents
6. **Error handling navigation** - Ajouter un guard/interceptor pour les pages détail avec ID invalide
7. **Notifications temps réel** - Le SSE est basique, pourrait utiliser WebSocket pour un meilleur UX

### P2 - Moyen terme

8. **Pagination backend** - Tous les endpoints de liste devraient supporter la pagination
9. **Recherche/Filtre côté serveur** - Les filtres actuels sont côté client - ne marchent pas pour les grosses données
10. **Optimistic locking** - Utiliser le champ `version` pour éviter les conflits de modification simultanée
11. **Export PDF** - Ajouter export PDF pour les factures et reçus
12. **Multi-devise** - Le systeme est basiquement multi-devises mais pas de taux de change
13. **Notifications push** - Ajouter push notifications mobiles pour les livraisons

### P3 - Long terme

14. **WebSocket** - Remplacer SSE par WebSocket pour les notifications temps réel
15. **GraphQL** - Les over-fetching sont fréquents - GraphQL pourrait optimiser
16. **Micro-frontends** - Si l'équipe grandit, découper en micro-frontends
17. **CQRS** - Séparer les modèles de lecture et d'écriture pour les lectures complexes
18. **Rate limiting** - Ajouter du rate limiting par rôle/utilisateur
19. **Audit trail UI** - Afficher l'audit log dans l'interface admin
20. **I18n** - Les textes sont hardcodés en français - internationaliser

---

## 9. Sécurité

### Points forts
- JWT avec validation côté gateway
- RBAC basé sur autorités Spring Security
- `@PreAuthorize` sur chaque endpoint
- Correlation ID pour le tracking des requêtes
- Outbox pattern pour la fiabilité des événements
- Event deduplication
- Audit logging

### Points à améliorer

| # | Risque | Fichier | Détail |
|---|--------|---------|--------|
| 1 | XSS | `payment-detail.component.html:26` | `qrdata` binding direct - si données compromisées |
| 2 | IDOR | `OrderController.java:176` | Vérification d'accès manuelle - pourrait être un filtre AOP |
| 3 | Rate limiting | `api-gateway/RateLimitFilter.java` | Présent mais config par défaut très permissive |
| 4 | Secrets | `application.yml` | Mots de passe en clair dans les configs dev/local |
| 5 | CORS | `SharedSecurityConfig.java` | Vérifier la config CORS en production |
| 6 | Input validation | `OrderController.java:232` | `assignDeliveryAgent` ne valide pas que agentId est bien un UUID valide |
| 7 | Admin bypass | `InternalAuthGuard.java` | Header `X-System-Admin: true` - à sécuriser en prod |

---

## 10. Performance

### Problèmes identifiés

| # | Impact | Fichier | Problème | Solution |
|---|--------|---------|----------|----------|
| 1 | Élevé | `OrderController.java:84-91` | 2 appels HTTP par commande dans buildOrderResponse | Batch resolver ou cache |
| 2 | Élevé | `OrderController.java:130` | PageResponse map pour chaque page - appels HTTP individuels | Pré-charger les noms |
| 3 | Moyen | `delivery-management.component.ts:166` | getShopAgents appelé à chaque ouverture modal | Cache TTL |
| 4 | Moyen | `order.service.ts list()` | Pas de pagination côté client | Ajouter pagination |
| 5 | Bas | `payment.service.ts list()` | Même problème pagination | Ajouter pagination |

### Optimisations suggérées

1. **Batch name resolution** - Créer un endpoint `/api/users/batch` qui résout plusieurs UUIDs en un seul appel
2. **Cache** - Utiliser Redis pour cacher les noms d'utilisateurs (TTL 5min)
3. **Pagination** - Tous les endpoints de liste devraient paginer (max 50 items par page)
4. **Lazy loading** - Charger les items/détails uniquement quand demandé
5. **Virtual scrolling** - Pour les listes longues côté frontend

---

## 11. UX/UI

### Points forts
- Design system cohérent (couleurs, badges, modals)
- Responsive design avec breakpoints
- Badges de statut colorés et intuitifs
- Timeline de progression pour les commandes
- QR Code pour le partage de factures

### Points à améliorer

| # | Problème | Composant | Solution |
|---|----------|-----------|----------|
| 1 | Pas de loading skeleton | Tous les tableaux | Ajouter des skeletons pendant le chargement |
| 2 | Pas de confirmation avant actions destructives | order-management | Les annulations utilisent `confirm()` natif - remplacer par un modal custom |
| 3 | Pas de toast de succès pour toutes les actions | delivery-management | Ajouter des feedbacks uniformes |
| 4 | Tabs non accessibles | order-management | Les boutons tab n'ont pas de `role="tab"` ni `aria-selected` |
| 5 | Pas de breadcrumb | Tous les détails | Ajouter un breadcrumb pour la navigation |
| 6 | Modal overlay sans focus trap | Tous les modals | Le focus peut sortir du modal avec Tab |
| 7 | Pas de empty state illustré | Tous les tableaux | Ajouter des illustrations pour les états vides |
| 8 | Dates non formatées uniformément | Tous les composants | Utiliser un pipe de date centralisé |
| 9 | Pas de dark mode | Global | Ajouter le thème sombre |
| 10 | Mobile: les tables débordent | table-card | Ajouter horizontal scroll ou cards sur mobile |

---

## 12. Architecture

### Recommandations

1. **Séparer IdentityClient en un Service dédié** - Le name resolution devrait être un micro-service ou un cache Redis dédié
2. **Event Sourcing** - Les OrderEvents pourraient être la source de vérité principale
3. **CQRS** - Séparer les modèles de lecture (détails, listes) des modèles d'écriture (commandes)
4. **Circuit Breaker** - Ajouter Resilience4j sur les appels inter-services (IdentityClient, PaymentClient)
5. **API Versioning** - Les endpoints n'ont pas de version (`/api/v1/...`)
6. **Health checks** - Ajouter des health checks détaillés (DB, RabbitMQ, services dépendants)
7. **Metrics** - Intégrer Micrometer + Prometheus pour le monitoring
8. **Structured logging** - Utiliser JSON logging pour le parsing ELK/EFK

---

## 13. Couverture des Tests

### Backend
| Module | Tests | Status |
|--------|-------|--------|
| shared-lib | 17 | ✅ Tous passent |
| identity-service | 22 | ✅ Tous passent |
| organization-service | 165 | ✅ Tous passent |
| payment-service | 7 | ✅ Tous passent |
| notification-service | 6 | ✅ Tous passent |
| **Total** | **217** | ✅ |

### Frontend
| Type | Tests | Status |
|------|-------|--------|
| Karma (unit) | 803 | ✅ Tous passent |
| Cypress (E2E) | 14 suites | ✅ |
| **Total** | **817+** | ✅ |

### Couverture estimée
- Backend: ~70% (use cases bien testés, manque tests d'intégration complets)
- Frontend: ~60% (composants testés, manque tests de bout en bout plus complets)
- E2E: ~40% (14 scénarios principaux couverts)

---

## 14. Résumé Statistique

| Métrique | Valeur |
|----------|--------|
| Modules backend | 6 |
| Fichiers Java | ~280 |
| Endpoints API | ~50 |
| Use Cases | ~30 |
| Entités JPA | ~20 |
| Migrations Liquibase | 10 |
| Composants Angular | 30 |
| Services Angular | 13 |
| Tests Karma | 803 |
| Tests Cypress | 14 suites |
| Tests Backend | 217 |
