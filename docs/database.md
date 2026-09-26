# Modèle de données MySQL

MySQL 8.4, InnoDB, utf8mb4, une base par microservice. Migrations **Liquibase**
(changelogs `db/changelog/V__*.sql` + `db.changelog-master.xml`, format
`--liquibase formatted sql`) versionnées dans chaque service. Aucune jointure inter-base.

## identity_db

```sql
CREATE TABLE users (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  username          VARCHAR(50)  NOT NULL UNIQUE,
  email             VARCHAR(255) NOT NULL UNIQUE,
  password_hash     VARCHAR(100) NOT NULL,
  first_name        VARCHAR(100) NOT NULL,
  last_name         VARCHAR(100) NOT NULL,
  phone             VARCHAR(30)  NULL,
  organization_id   BIGINT       NULL,           -- réf. organization_db (pas de FK inter-base)
  status            VARCHAR(20)  NOT NULL,       -- ACTIVE | DISABLED
  version           BIGINT       NOT NULL DEFAULT 0, -- optimistic locking
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL
);

CREATE TABLE roles (
  code  VARCHAR(30) PRIMARY KEY,   -- SYSTEM_ADMIN, SUPPLIER_ADMIN, SUPPLIER_AGENT, SHOP_ADMIN, SHOP_AGENT
  name  VARCHAR(100) NOT NULL
);

CREATE TABLE user_roles (
  user_id  BIGINT NOT NULL,
  role_code VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role_code)
);

CREATE TABLE permissions (
  code VARCHAR(60) PRIMARY KEY
);

CREATE TABLE role_permissions (
  role_code       VARCHAR(30) NOT NULL,
  permission_code VARCHAR(60) NOT NULL,
  PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE audit_logs (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT NULL,
  organization_id BIGINT NULL,
  action          VARCHAR(60) NOT NULL,
  entity_id       BIGINT NULL,
  timestamp       DATETIME(6) NOT NULL,
  details         JSON NULL,
  INDEX idx_audit_action (action), INDEX idx_audit_ts (timestamp)
);

CREATE TABLE outbox_events (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id      CHAR(36) NOT NULL UNIQUE,
  event_type    VARCHAR(120) NOT NULL,
  aggregate_id  VARCHAR(64) NOT NULL,
  payload       JSON NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  processed_at  DATETIME(6) NULL,
  INDEX idx_outbox_processed (processed_at)
);

-- M1 (V6__refresh_tokens.sql, Liquibase) : rotation + révocation.
-- Seul le hash SHA-256 est persisté, jamais le token brut. TTL applicatif 7 j.
CREATE TABLE refresh_tokens (
  id          VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  user_id     VARCHAR(36) NOT NULL,
  expires_at  DATETIME(6) NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT FALSE,
  replaced_by VARCHAR(64) NULL,
  created_at  DATETIME(6) NOT NULL,
  INDEX idx_refresh_tokens_user (user_id),
  INDEX idx_refresh_tokens_expires (expires_at)
);
```

Changelogs identity (Liquibase) : `V1__schema` → `V2__seed_roles` →
`V3__seed_permissions` → `V4__seed_users` (+ `V5__fix_seed_password`) →
`V6__refresh_tokens` (M1). B4 volontairement non corrigé : seed `Admin@123`
(hash BCrypt identique ×10) toujours présent — réservé aux profils non-prod.

## organization_db

```sql
CREATE TABLE organizations (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  type        VARCHAR(20)  NOT NULL,   -- SUPPLIER | SHOP
  status      VARCHAR(20)  NOT NULL,   -- ACTIVE | DISABLED
  version     BIGINT NOT NULL DEFAULT 0,
  created_at  DATETIME(6) NOT NULL,
  updated_at  DATETIME(6) NOT NULL
);

CREATE TABLE supplier_shop_relations (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT NOT NULL,
  shop_id     BIGINT NOT NULL,
  status      VARCHAR(20) NOT NULL,    -- ACTIVE | DISABLED
  created_at  DATETIME(6) NOT NULL,
  UNIQUE KEY uq_relation (supplier_id, shop_id)
);

-- + audit_logs, outbox_events (identiques à identity_db)

-- M3 (V10__catalog_soft_delete.sql) : soft-delete catalogue.
ALTER TABLE product_categories ADD COLUMN deleted_at DATETIME(6) NULL;
ALTER TABLE product_families  ADD COLUMN deleted_at DATETIME(6) NULL;
-- Les requêtes filtrent `deletedAt IS NULL` ; l'historique (produits, commandes) survit.
```

## payment_db

```sql
CREATE TABLE payments (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  reference        VARCHAR(64) NOT NULL UNIQUE,
  shop_id          BIGINT NOT NULL,     -- organization type SHOP
  supplier_id      BIGINT NOT NULL,     -- organization type SUPPLIER
  currency         CHAR(3) NOT NULL,
  amount           DECIMAL(19,4) NOT NULL,
  status           VARCHAR(20) NOT NULL, -- PENDING | CONFIRMED | REJECTED | CANCELLED
  rejection_reason VARCHAR(500) NULL,
  created_by       BIGINT NOT NULL,      -- user identity
  version          BIGINT NOT NULL DEFAULT 0,
  created_at       DATETIME(6) NOT NULL,
  updated_at       DATETIME(6) NOT NULL,
  INDEX idx_payments_supplier (supplier_id), INDEX idx_payments_shop (shop_id),
  INDEX idx_payments_status (status)
);

-- B1 (V3__add_idempotency_key.sql) : clé persistée + contrainte unique.
-- ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(64) NULL;
-- CREATE UNIQUE INDEX ux_payments_idempotency_key ON payments(idempotency_key);

CREATE TABLE payment_events (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_id  BIGINT NOT NULL,
  action      VARCHAR(40) NOT NULL,
  user_id     BIGINT NULL,
  timestamp   DATETIME(6) NOT NULL,
  details     JSON NULL
);

-- + audit_logs, outbox_events (identiques)
```

## notification_db

```sql
CREATE TABLE notifications (
  id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_user_id         BIGINT NOT NULL,
  recipient_organization_id BIGINT NULL,
  type                      VARCHAR(40) NOT NULL,  -- PAYMENT_CREATED, PAYMENT_CONFIRMED, ...
  message                   VARCHAR(500) NOT NULL,
  read_status               VARCHAR(20) NOT NULL,   -- UNREAD | READ
  created_at                DATETIME(6) NOT NULL,
  read_at                   DATETIME(6) NULL,
  INDEX idx_notif_user (recipient_user_id, read_status)
);

-- + outbox_events (identiques)
```

## Notes

- `DECIMAL(19,4)` pour l'argent ; `CHAR(3)` ISO 4217 pour les devises.
- Les `organization_id`/`user_id` référencés entre bases sont des **références logiques** (pas de contrainte FK inter-base) — cohérence garantie par événements.
- Versioning schéma : Liquibase (migrations additives uniquement en production).
  Changelogs vérifiés : payment `V1__schema, V2__add_order_payment_terms, V3__add_idempotency_key (B1)` ;
  organization `V1..V9 + V10__catalog_soft_delete (M3)` ; notification `V1__schema, V2__fcm_tokens, V3__push_tokens`.