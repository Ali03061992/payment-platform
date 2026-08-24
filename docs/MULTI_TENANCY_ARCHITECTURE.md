# Architecture Multi-Tenancy — Base de Données

## 1. Vue d'ensemble

La plateforme de paiement utilise une architecture **multi-tenant par données (shared database, row-level isolation)**. Toutes les données de tous les clients (fournisseurs et boutiques) coexistent dans les mêmes bases de données MySQL, séparées par microservice. L'isolation est garantie par des colonnes de clé de tenant dans chaque table et par la couche applicative.

```
┌─────────────────────────────────────────────────────────────┐
│                    SYSTEM_ADMIN (global)                     │
│         Voit TOUT — Pas de scope organizationnel            │
└─────────────┬───────────────────────────────┬───────────────┘
              │                               │
   ┌──────────▼──────────┐       ┌────────────▼────────────┐
   │  FOURNISSEUR (A)    │       │     BOUTIQUE (X)        │
   │  scope: supplier_id │       │    scope: shop_id       │
   │  - Produits         │       │    - Commandes          │
   │  - Stock            │       │    - Paiements          │
   │  - Catégories       │       │    - Balance            │
   └──────────┬──────────┘       └────────────┬────────────┘
              │                               │
              └───────────┬───────────────────┘
                          │
              ┌───────────▼───────────────────┐
              │  RELATION (A ↔ X)             │
              │  scope: supplier_id + shop_id │
              │  - Commandes liées            │
              │  - Balance                    │
              │  - Paiements                  │
              └───────────────────────────────┘
```

## 2. Topologie des Bases de Données

Chaque microservice possède sa propre base de données MySQL :

| Base de données      | Microservice         | Port  | Contenu principal                      |
|---------------------|---------------------|-------|----------------------------------------|
| `identity_db`       | identity-service    | 3306  | Utilisateurs, rôles, permissions       |
| `organization_db`   | organization-service| 3306  | Organisations, produits, commandes, balance |
| `payment_db`        | payment-service     | 3306  | Paiements                              |
| `notification_db`   | notification-service| 3306  | Notifications                          |

> **Note** : Les tables `audit_logs`, `outbox_events`, `processed_events` (shared-lib) sont dupliquées dans chaque base — elles ne sont pas centralisées.

## 3. Modèle de Clés de Tenant

### 3.1. Schéma des clés

```
┌──────────────────────────────────────────────────────────────────┐
│                     organizations (global)                        │
│  id (PK) │ name │ type (SUPPLIER|SHOP) │ status                 │
└──────┬───────────────────────────────────────────────────────────┘
       │
       │  supplier_id ────┐
       │                  ├──→ supplier_shop_relations
       │  shop_id ────────┘
       │
       │  supplier_id (seul)          supplier_id + shop_id (ensemble)
       │  ┌─────────────────┐        ┌──────────────────────────────┐
       │  │ products        │        │ orders                       │
       │  │ stock_movements │        │ balance_ledger               │
       │  │ product_cat.    │        │ payments                     │
       │  │ product_fam.    │        │                              │
       │  │ product_subfam. │        │                              │
       │  └─────────────────┘        └──────────────────────────────┘
       │
       │  organization_id (nullable)
       │  ┌─────────────────┐
       │  │ users           │  (SYSTEM_ADMIN → NULL = global)
       │  │ notifications   │
       │  │ audit_logs      │
       │  └─────────────────┘
       │
       └──→ Aucune clé FK inter-database
            (intégrité applicative uniquement)
```

### 3.2. Niveaux d'isolation par table

| Table                        | Clé de tenant              | Isolation       | Propriétaire service     |
|------------------------------|---------------------------|-----------------|--------------------------|
| `organizations`              | Aucune (global)           | Tous voient     | organization-service     |
| `roles` / `permissions`      | Aucune (global)           | Tous voient     | identity-service         |
| `users`                      | `organization_id` (nullable) | Par org      | identity-service         |
| `products`                   | `supplier_id`             | Par fournisseur | organization-service     |
| `product_categories`         | `supplier_id`             | Par fournisseur | organization-service     |
| `product_families`           | `supplier_id`             | Par fournisseur | organization-service     |
| `product_subfamilies`        | `supplier_id`             | Par fournisseur | organization-service     |
| `stock_movements`            | `supplier_id`             | Par fournisseur | organization-service     |
| `supplier_shop_relations`    | `supplier_id` + `shop_id` | Par paire       | organization-service     |
| `orders`                     | `supplier_id` + `shop_id` | Par paire       | organization-service     |
| `order_items`                | Via parent `order_id`     | Hérité          | organization-service     |
| `order_events`               | Via parent `order_id`     | Hérité          | organization-service     |
| `balance_ledger`             | `supplier_id` + `shop_id` | Par paire       | organization-service     |
| `payments`                   | `supplier_id` + `shop_id` | Par paire       | payment-service          |
| `payment_events`             | Via parent `payment_id`   | Hérité          | payment-service          |
| `notifications`              | `recipient_organization_id` (nullable) | Par org | notification-service    |

## 4. Scopes de Données par Rôle

### 4.1. SYSTEM_ADMIN

```
┌─────────────────────────────────────────────┐
│  SYSTEM_ADMIN                                │
│  ─────────────                               │
│  organization_id = NULL (global)             │
│                                              │
│  ✅ VOIT TOUT :                              │
│  - Toutes les organisations                  │
│  - Tous les utilisateurs                     │
│  - Toutes les commandes                      │
│  - Tous les paiements                        │
│  - Toutes les balances                       │
│  - Tous les produits                         │
│  - Peut créer tout type de compte            │
│  - Peut activer/désactiver tout              │
└─────────────────────────────────────────────┘
```

### 4.2. SUPPLIER_ADMIN

```
┌─────────────────────────────────────────────┐
│  SUPPLIER_ADMIN (org_id = 100)               │
│  ────────────────────────                    │
│  scope : supplier_id = 100                   │
│                                              │
│  ✅ PEUT VOIR/FAIRE :                        │
│  - Ses propres produits (catalogue)          │
│  - Son stock et mouvements                   │
│  - Les commandes le concernant               │
│  - Les paiements le concernant               │
│  - La balance de chaque boutique liée        │
│  - Gérer ses agents (SUPPLIER_AGENT)         │
│  - Créer des produits                        │
│  - Confirmer/Préparer des commandes          │
│  - Assigner un livreur                       │
│                                              │
│  ❌ NE PEUT PAS VOIR :                       │
│  - Les autres fournisseurs                   │
│  - Les boutiques non liées                   │
│  - Les comptes d'autres organisations        │
└─────────────────────────────────────────────┘
```

### 4.3. SUPPLIER_AGENT

```
┌─────────────────────────────────────────────┐
│  SUPPLIER_AGENT (org_id = 100)               │
│  ────────────────────────                    │
│  scope : supplier_id = 100                   │
│                                              │
│  ✅ PEUT VOIR :                              │
│  - Les produits de son fournisseur           │
│  - Le stock (lecture + mouvements)           │
│  - Les livraisons qui lui sont assignées     │
│                                              │
│  ❌ NE PEUT PAS :                            │
│  - Créer/supprimer des produits              │
│  - Gérer les commandes                       │
│  - Voir les paiements                        │
│  - Gérer d'autres agents                     │
└─────────────────────────────────────────────┘
```

### 4.4. SHOP_ADMIN

```
┌─────────────────────────────────────────────┐
│  SHOP_ADMIN (org_id = 200)                   │
│  ────────────────────                        │
│  scope : shop_id = 200                       │
│                                              │
│  ✅ PEUT VOIR/FAIRE :                        │
│  - Ses commandes (avec tous ses fournisseurs)│
│  - Créer des commandes                       │
│  - Réceptionner les livraisons               │
│  - Accepter les livraisons                   │
│  - Les paiements (créer, consulter)          │
│  - La balance avec chaque fournisseur        │
│  - Gérer ses agents (SHOP_AGENT)             │
│                                              │
│  ❌ NE PEUT PAS VOIR :                       │
│  - Les autres boutiques                      │
│  - Les produits (catalogue fournisseur)      │
│  - Les stocks des fournisseurs               │
└─────────────────────────────────────────────┘
```

### 4.5. SHOP_AGENT

```
┌─────────────────────────────────────────────┐
│  SHOP_AGENT (org_id = 200)                   │
│  ────────────────────                        │
│  scope : shop_id = 200                       │
│                                              │
│  ✅ PEUT VOIR (lecture seule) :               │
│  - Les commandes de sa boutique              │
│  - Les paiements de sa boutique              │
│                                              │
│  ❌ NE PEUT PAS :                            │
│  - Créer des commandes                       │
│  - Créer des paiements                       │
│  - Gérer les livraisons                      │
│  - Accepter les commandes                    │
└─────────────────────────────────────────────┘
```

## 5. Flux de Données Multi-Tenant

### 5.1. Création d'une commande

```
Shop Admin (org_id=200) crée une commande
         │
         ▼
POST /api/orders { supplierId: 100, shopId: 200, items: [...] }
         │
         ▼
┌─────────────────────────────────────────────────┐
│ organization_db.orders                           │
│   supplier_id = 100  ← scope du fournisseur     │
│   shop_id = 200      ← scope de la boutique     │
│   status = PENDING                               │
└─────────────────────────────────────────────────┘
         │
         ▼ (event: ORDER_CREATED)
         │
┌─────────────────────────────────────────────────┐
│ organization_db.order_items                      │
│   order_id → orders.id                          │
│   (pas de clé propre — isolation via parent)     │
└─────────────────────────────────────────────────┘
         │
         ▼ (event: ORDER_CREATED via RabbitMQ)
         │
┌─────────────────────────────────────────────────┐
│ organization_db.stock_movements                  │
│   product_id → products.id                      │
│   supplier_id = 100                             │
│   type = 'RESERVATION'                          │
│   quantity = -N                                 │
└─────────────────────────────────────────────────┘
```

### 5.2. Livraison et réception

```
Supplier Agent (org_id=100) livre la commande
         │
         ▼
PATCH /api/orders/{id}/deliver
         │
         ▼
┌─────────────────────────────────────────────────┐
│ organization_db.orders                           │
│   status = DELIVERED                             │
│   delivered_at = now()                           │
└─────────────────────────────────────────────────┘
         │
         ▼ (Shop Admin accepte)
         │
PATCH /api/orders/{id}/accept
         │
         ▼
┌─────────────────────────────────────────────────┐
│ organization_db.orders                           │
│   status = ACCEPTED                              │
│   received_at = now()                            │
├─────────────────────────────────────────────────┤
│ organization_db.stock_movements                  │
│   type = 'OUT' (déduction définitive du stock)   │
├─────────────────────────────────────────────────┤
│ organization_db.balance_ledger                   │
│   supplier_id = 100, shop_id = 200               │
│   type = 'ORDER_CREDIT'                          │
│   amount = +total_commande                       │
│   balance_after = ancien_solde + total           │
├─────────────────────────────────────────────────┤
│ (si ASAP) payment_db.payments                    │
│   supplier_id = 100, shop_id = 200               │
│   status = CONFIRMED                             │
└─────────────────────────────────────────────────┘
```

## 6. Garanties d'Isolation

### 6.1. Couche applicative (current)

L'isolation est actuellement **entièrement gérée par le code applicatif** :

```java
// Exemple : UserQueryUseCase
boolean isSystemAdmin = actorRoles.contains("SYSTEM_ADMIN");
if (isSystemAdmin) {
    result = users.findAll();  // pas de filtre
} else {
    result = users.findByOrganizationId(actorOrganizationId);  // filtré
}

// Exemple : OrderRepository
List<Order> findBySupplierIdAndShopId(Long supplierId, Long shopId);
// Le contrôleur passe toujours le scope du user connecté
```

**Risques :**
- Un bug dans le code peut exposer des données d'un autre tenant
- Pas de protection au niveau SQL
- Dépend de la rigueur de chaque développeur

### 6.2. Améliorations possibles (futur)

| Approche | Niveau | Complexité | Impact |
|----------|--------|------------|--------|
| **RLS (Row-Level Security)** | Fort | Élevée | Filtrage SQL automatique par role MySQL |
| **Vues materialisées par tenant** | Moyen | Moyenne | Copie des données par tenant |
| **Schema par tenant** | Fort | Très élevée | Une base/schema par client |
| **Column-level encryption** | Moyen | Moyenne | Données chiffrées par tenant |

#### Option recommandée : Column-level tenant isolation

```sql
-- Ajouter une colonne tenant_id à toutes les tables
ALTER TABLE orders ADD COLUMN tenant_id BIGINT NOT NULL;
CREATE INDEX idx_orders_tenant ON orders(tenant_id);

-- Créer un role MySQL par scope
CREATE ROLE 'supplier_100';
GRANT SELECT, INSERT, UPDATE ON organization_db.orders TO 'supplier_100';
-- RLS ou WHERE clause automatique via vues
```

## 7. Schéma Récapitulatif des Tables

### organization_db

```sql
-- Tables globales (pas de tenant scope)
organizations          -- id, name, type, status
supplier_shop_relations -- supplier_id, shop_id, status

-- Scope fournisseur (supplier_id)
products               -- supplier_id, sku, name, quantity, reserved_qty
product_categories     -- supplier_id, name, code
product_families       -- supplier_id, category_id, name, code
product_subfamilies    -- supplier_id, family_id, name, code
stock_movements        -- supplier_id, product_id, type, quantity

-- Scope paire (supplier_id + shop_id)
orders                 -- supplier_id, shop_id, reference, status, total
order_items            -- order_id (hérité), product_id, quantity, unit_price
order_events           -- order_id (hérité), action, user_id
balance_ledger         -- supplier_id, shop_id, type, amount, balance_after
```

### identity_db

```sql
-- Tables globales
roles                  -- code, name
permissions            -- code
role_permissions       -- role_code, permission_code

-- Scope organisation (organization_id nullable)
users                  -- organization_id, username, email, status
user_roles             -- user_id, role_code
password_setup_tokens  -- user_id, token, expires_at
```

### payment_db

```sql
-- Scope paire (supplier_id + shop_id)
payments               -- supplier_id, shop_id, reference, amount, status
payment_events         -- payment_id (hérité), action, user_id
```

### notification_db

```sql
-- Scope organisation (recipient_organization_id nullable)
notifications          -- recipient_user_id, recipient_organization_id, type, message
```

## 8. Index de Performance par Tenant

Toutes les tables isolées par tenant ont un index sur leur clé de tenant :

```sql
-- Index standard sur chaque table scope fournisseur
CREATE INDEX idx_products_supplier ON products(supplier_id);
CREATE INDEX idx_stock_movements_supplier ON stock_movements(supplier_id);

-- Index standard sur chaque table scope paire
CREATE INDEX idx_orders_supplier ON orders(supplier_id);
CREATE INDEX idx_orders_shop ON orders(shop_id);
CREATE INDEX idx_balance_supplier ON balance_ledger(supplier_id);
CREATE INDEX idx_balance_shop ON balance_ledger(shop_id);
CREATE INDEX idx_payments_supplier ON payments(supplier_id);
CREATE INDEX idx_payments_shop ON payments(shop_id);

-- Index composites pour requêtes fréquentes
CREATE UNIQUE INDEX uq_supplier_sku ON products(supplier_id, sku);
CREATE UNIQUE INDEX uq_relation ON supplier_shop_relations(supplier_id, shop_id);
CREATE UNIQUE INDEX uq_order_ref ON orders(reference);
CREATE UNIQUE INDEX uq_payment_ref ON payments(reference);
```

## 9. Résumé des Règles Métier Multi-Tenant

| Règle | Implémentation |
|-------|----------------|
| Un produit appartient à un seul fournisseur | `products.supplier_id` FK logique |
| Une commande concerne un fournisseur + une boutique | `orders.{supplier_id, shop_id}` |
| Le stock est réservé à la confirmation, déduit à l'acceptation | `reserved_qty` → `quantity` via `stock_movements` |
| La balance est un journal immuable par paire | `balance_ledger` append-only |
| Les paiements partiels sont autorisés | Plusieurs paiements par commande |
| Le prix historique est conservé dans la commande | `order_items.unit_price` figé |
| SYSTEM_ADMIN voit tout, les autres sont scope | Filtrage applicatif par `organization_id` / `supplier_id` / `shop_id` |
| Un fournisseur est exclusif par produit | `uq_supplier_sku` contrainte unique |
| Les boutiques peuvent avoir plusieurs fournisseurs | Relation many-to-many via `supplier_shop_relations` |
