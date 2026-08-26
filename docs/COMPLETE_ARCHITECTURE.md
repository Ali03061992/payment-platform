# Complete Technical Architecture — B2B Payment Platform

> Stack: Java 26 · Spring Boot 4.1 · Angular 16 · MySQL 8.4 · Docker · RabbitMQ 4 · JWT (HS256)

---

## 1. MICROSERVICE DECOMPOSITION

### 1.1 Service → Bounded Context Mapping

| Service | Bounded Context | Aggregates Owned | Database |
|---|---|---|---|
| `api-gateway` | — (routing proxy) | — | — |
| `identity-service` | Identity | User, Role, Permission, Session | `identity_db` |
| `organization-service` | Organization | Organization, SupplierShopRelation, Product, Order, BalanceLedger | `organization_db` |
| `payment-service` | Payment | Payment | `payment_db` |
| `notification-service` | Notification | Notification | `notification_db` |

### 1.2 Justification

- **Identity** is isolated because credential management, RBAC, and JWT issuance are security-critical and must change at a different cadence than business logic.
- **Organization** owns the supplier/shop hierarchy, catalog, orders, and balance because these form a single consistency boundary (an order references products, stock, and balance within one transaction).
- **Payment** is a separate aggregate because payments have their own lifecycle (PENDING → CONFIRMED/REJECTED/CANCELLED), optimistic locking, and must never be co-located with order/stock writes (different scaling profile, different audit requirements).
- **Notification** is a pure consumer — it persists notifications and pushes SSE. No other service depends on it.

### 1.3 Communication Patterns

```
SYNCHRONOUS (REST via API Gateway)
──────────────────────────────────
Frontend ──▶ API Gateway ──▶ each service
Payment ──▶ Organization  (validate relation + org status at payment creation)

ASYNCHRONOUS (RabbitMQ events)
──────────────────────────────
Identity    ──Event──▶ Notification   (UserCreated/Disabled)
Organization──Event──▶ Identity       (Supplier/Shop Disabled → cascade)
Organization──Event──▶ Notification   (all org events)
Payment     ──Event──▶ Notification   (PaymentCreated/Confirmed/Rejected/Cancelled)
```

**Rule**: No direct service-to-service REST calls. Inter-service synchronous calls route through the Gateway (ADR-02 in architecture.md). Async events use the Outbox pattern.

### 1.4 Database Ownership

| Database | Tables | Owner |
|---|---|---|
| `identity_db` | `users`, `roles`, `user_roles`, `permissions`, `role_permissions`, `password_setup_tokens`, `audit_logs`, `outbox_events` | identity-service |
| `organization_db` | `organizations`, `supplier_shop_relations`, `products`, `product_categories`, `product_families`, `product_subfamilies`, `stock_movements`, `orders`, `order_items`, `order_events`, `balance_ledger`, `audit_logs`, `outbox_events` | organization-service |
| `payment_db` | `payments`, `payment_events`, `audit_logs`, `outbox_events` | payment-service |
| `notification_db` | `notifications`, `processed_events` | notification-service |

No foreign keys across databases. All cross-service references are logical (application-level integrity via events).

---

## 2. DATABASE SCHEMA

### 2.1 identity_db

```sql
-- V1__init_identity.sql

CREATE TABLE users (
  id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
  username          VARCHAR(50)  NOT NULL,
  email             VARCHAR(255) NOT NULL,
  password_hash     VARCHAR(100) NOT NULL,
  first_name        VARCHAR(100) NOT NULL,
  last_name         VARCHAR(100) NOT NULL,
  phone             VARCHAR(30)  NULL,
  organization_id   BIGINT       NULL,
  status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  version           BIGINT       NOT NULL DEFAULT 0,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  CONSTRAINT uq_username UNIQUE (username),
  CONSTRAINT uq_email    UNIQUE (email),
  CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_org        ON users(organization_id);
CREATE INDEX idx_users_status     ON users(status);
CREATE INDEX idx_users_org_status ON users(organization_id, status);

CREATE TABLE roles (
  code VARCHAR(30)  PRIMARY KEY,
  name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO roles (code, name) VALUES
  ('SYSTEM_ADMIN','System Administrator'),
  ('SUPPLIER_ADMIN','Supplier Administrator'),
  ('SUPPLIER_AGENT','Supplier Agent'),
  ('SHOP_ADMIN','Shop Administrator'),
  ('SHOP_AGENT','Shop Agent');

CREATE TABLE permissions (
  code VARCHAR(60) PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permissions (code) VALUES
  ('ADMIN_MANAGE_ORGANIZATIONS'),
  ('ADMIN_MANAGE_USERS'),
  ('ADMIN_VIEW_AUDIT'),
  ('ADMIN_VIEW_STATS'),
  ('SUPPLIER_MANAGE_AGENTS'),
  ('SUPPLIER_MANAGE_PAYMENTS'),
  ('SUPPLIER_MANAGE_CATALOG'),
  ('SUPPLIER_MANAGE_ORDERS'),
  ('SHOP_MANAGE_AGENTS'),
  ('SHOP_CREATE_PAYMENTS'),
  ('SHOP_CANCEL_PAYMENTS'),
  ('SHOP_MANAGE_ORDERS'),
  ('VIEW_PAYMENTS'),
  ('VIEW_NOTIFICATIONS');

CREATE TABLE user_roles (
  user_id   BIGINT      NOT NULL,
  role_code VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
  role_code       VARCHAR(30) NOT NULL,
  permission_code VARCHAR(60) NOT NULL,
  PRIMARY KEY (role_code, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE password_setup_tokens (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT       NOT NULL,
  token      VARCHAR(64)  NOT NULL,
  expires_at DATETIME(6)  NOT NULL,
  used       BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at DATETIME(6)  NOT NULL,
  CONSTRAINT uq_pst_token UNIQUE (token),
  INDEX idx_pst_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NULL,
  organization_id BIGINT       NULL,
  action          VARCHAR(60)  NOT NULL,
  entity_type     VARCHAR(60)  NULL,
  entity_id       BIGINT       NULL,
  timestamp       DATETIME(6)  NOT NULL,
  details         JSON         NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts     (timestamp),
  INDEX idx_audit_user   (user_id),
  INDEX idx_audit_org    (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
  event_id     CHAR(36)     NOT NULL,
  event_type   VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64)  NOT NULL,
  payload      JSON         NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  processed_at DATETIME(6)  NULL,
  CONSTRAINT uq_outbox_event_id UNIQUE (event_id),
  INDEX idx_outbox_unprocessed (processed_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.2 organization_db

```sql
-- V1__init_organization.sql

CREATE TABLE organizations (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  type       VARCHAR(20)  NOT NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  version    BIGINT       NOT NULL DEFAULT 0,
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  CONSTRAINT ck_org_type   CHECK (type IN ('SUPPLIER','SHOP')),
  CONSTRAINT ck_org_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_org_type   ON organizations(type);
CREATE INDEX idx_org_status ON organizations(status);

CREATE TABLE supplier_shop_relations (
  id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT      NOT NULL,
  shop_id     BIGINT      NOT NULL,
  status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at  DATETIME(6) NOT NULL,
  updated_at  DATETIME(6) NOT NULL,
  CONSTRAINT uq_relation  UNIQUE (supplier_id, shop_id),
  CONSTRAINT ck_rel_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_rel_supplier ON supplier_shop_relations(supplier_id);
CREATE INDEX idx_rel_shop     ON supplier_shop_relations(shop_id);

CREATE TABLE product_categories (
  id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
  supplier_id  BIGINT       NOT NULL,
  name         VARCHAR(100) NOT NULL,
  code         VARCHAR(30)  NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  CONSTRAINT uq_cat_supplier_code UNIQUE (supplier_id, code),
  INDEX idx_cat_supplier (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_families (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT       NOT NULL,
  category_id BIGINT       NOT NULL,
  name        VARCHAR(100) NOT NULL,
  code        VARCHAR(30)  NOT NULL,
  created_at  DATETIME(6)  NOT NULL,
  CONSTRAINT uq_fam_supplier_code UNIQUE (supplier_id, code),
  INDEX idx_fam_supplier (supplier_id),
  INDEX idx_fam_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_subfamilies (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT       NOT NULL,
  family_id   BIGINT       NOT NULL,
  name        VARCHAR(100) NOT NULL,
  code        VARCHAR(30)  NOT NULL,
  created_at  DATETIME(6)  NOT NULL,
  CONSTRAINT uq_subfam_supplier_code UNIQUE (supplier_id, code),
  INDEX idx_subfam_supplier (supplier_id),
  INDEX idx_subfam_family   (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
  id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
  supplier_id    BIGINT        NOT NULL,
  sku            VARCHAR(50)   NOT NULL,
  name           VARCHAR(200)  NOT NULL,
  description    VARCHAR(2000) NULL,
  category_id    BIGINT        NULL,
  family_id      BIGINT        NULL,
  subfamily_id   BIGINT        NULL,
  unit_price     DECIMAL(19,4) NOT NULL,
  currency       CHAR(3)       NOT NULL,
  quantity       INT           NOT NULL DEFAULT 0,
  reserved_qty   INT           NOT NULL DEFAULT 0,
  unit           VARCHAR(20)   NULL,
  status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  version        BIGINT        NOT NULL DEFAULT 0,
  created_at     DATETIME(6)   NOT NULL,
  updated_at     DATETIME(6)   NOT NULL,
  CONSTRAINT uq_supplier_sku UNIQUE (supplier_id, sku),
  CONSTRAINT ck_prod_status   CHECK (status IN ('ACTIVE','DISABLED')),
  CONSTRAINT ck_price_positive CHECK (unit_price >= 0),
  CONSTRAINT ck_qty_nonneg     CHECK (quantity >= 0),
  CONSTRAINT ck_reserved_nonneg CHECK (reserved_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_prod_supplier (products(supplier_id));
CREATE INDEX idx_prod_category (products(category_id));
CREATE INDEX idx_prod_status   (products(status));

CREATE TABLE stock_movements (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT       NOT NULL,
  product_id  BIGINT       NOT NULL,
  type        VARCHAR(30)  NOT NULL,
  quantity    INT          NOT NULL,
  reference   VARCHAR(64)  NULL,
  note        VARCHAR(500) NULL,
  created_by  BIGINT       NULL,
  created_at  DATETIME(6)  NOT NULL,
  CONSTRAINT ck_movement_type CHECK (type IN ('IN','OUT','RESERVATION','RELEASE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_stock_supplier  ON stock_movements(supplier_id);
CREATE INDEX idx_stock_product   ON stock_movements(product_id);
CREATE INDEX idx_stock_type      ON stock_movements(type);
CREATE INDEX idx_stock_created   ON stock_movements(created_at);

CREATE TABLE orders (
  id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
  reference    VARCHAR(64)   NOT NULL,
  supplier_id  BIGINT        NOT NULL,
  shop_id      BIGINT        NOT NULL,
  status       VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
  total        DECIMAL(19,4) NOT NULL DEFAULT 0,
  currency     CHAR(3)       NOT NULL,
  note         VARCHAR(2000) NULL,
  created_by   BIGINT        NOT NULL,
  delivered_at DATETIME(6)   NULL,
  received_at  DATETIME(6)   NULL,
  version      BIGINT        NOT NULL DEFAULT 0,
  created_at   DATETIME(6)   NOT NULL,
  updated_at   DATETIME(6)   NOT NULL,
  CONSTRAINT uq_order_ref UNIQUE (reference),
  CONSTRAINT ck_order_status CHECK (status IN (
    'PENDING','CONFIRMED','PREPARING','DELIVERED','ACCEPTED','REJECTED','CANCELLED'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_order_supplier ON orders(supplier_id);
CREATE INDEX idx_order_shop     ON orders(shop_id);
CREATE INDEX idx_order_status   ON orders(status);
CREATE INDEX idx_order_created  ON orders(created_at);

CREATE TABLE order_items (
  id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
  order_id    BIGINT        NOT NULL,
  product_id  BIGINT        NOT NULL,
  sku         VARCHAR(50)   NOT NULL,
  name        VARCHAR(200)  NOT NULL,
  quantity    INT           NOT NULL,
  unit_price  DECIMAL(19,4) NOT NULL,
  CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
  CONSTRAINT ck_oi_qty_pos CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_oi_order ON order_items(order_id);

CREATE TABLE order_events (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT       NOT NULL,
  action     VARCHAR(40)  NOT NULL,
  user_id    BIGINT       NULL,
  timestamp  DATETIME(6)  NOT NULL,
  details    JSON         NULL,
  CONSTRAINT fk_oe_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_oe_order ON order_events(order_id);

CREATE TABLE balance_ledger (
  id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
  supplier_id    BIGINT        NOT NULL,
  shop_id        BIGINT        NOT NULL,
  type           VARCHAR(30)   NOT NULL,
  amount         DECIMAL(19,4) NOT NULL,
  balance_after  DECIMAL(19,4) NOT NULL,
  reference_type VARCHAR(30)  NULL,
  reference_id   BIGINT        NULL,
  note           VARCHAR(500)  NULL,
  created_at     DATETIME(6)   NOT NULL,
  CONSTRAINT ck_bl_type CHECK (type IN (
    'ORDER_CREDIT','ORDER_DEBIT','PAYMENT_CREDIT','ADJUSTMENT'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bl_supplier ON balance_ledger(supplier_id);
CREATE INDEX idx_bl_shop     ON balance_ledger(shop_id);
CREATE INDEX idx_bl_pair     ON balance_ledger(supplier_id, shop_id);
CREATE INDEX idx_bl_type     ON balance_ledger(type);
CREATE INDEX idx_bl_created  ON balance_ledger(created_at);

CREATE TABLE audit_logs (
  id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NULL,
  organization_id BIGINT       NULL,
  action          VARCHAR(60)  NOT NULL,
  entity_type     VARCHAR(60)  NULL,
  entity_id       BIGINT       NULL,
  timestamp       DATETIME(6)  NOT NULL,
  details         JSON         NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts     (timestamp),
  INDEX idx_audit_user   (user_id),
  INDEX idx_audit_org    (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
  event_id     CHAR(36)     NOT NULL,
  event_type   VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64)  NOT NULL,
  payload      JSON         NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  processed_at DATETIME(6)  NULL,
  CONSTRAINT uq_outbox_event_id UNIQUE (event_id),
  INDEX idx_outbox_unprocessed (processed_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.3 payment_db

```sql
-- V1__init_payment.sql

CREATE TABLE payments (
  id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
  reference        VARCHAR(64)   NOT NULL,
  shop_id          BIGINT        NOT NULL,
  supplier_id      BIGINT        NOT NULL,
  currency         CHAR(3)       NOT NULL,
  amount           DECIMAL(19,4) NOT NULL,
  status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  rejection_reason VARCHAR(500)  NULL,
  comment          VARCHAR(2000) NULL,
  created_by       BIGINT        NOT NULL,
  confirmed_by     BIGINT        NULL,
  version          BIGINT        NOT NULL DEFAULT 0,
  created_at       DATETIME(6)   NOT NULL,
  updated_at       DATETIME(6)   NOT NULL,
  CONSTRAINT uq_payment_ref   UNIQUE (reference),
  CONSTRAINT ck_payment_status CHECK (status IN ('PENDING','CONFIRMED','REJECTED','CANCELLED')),
  CONSTRAINT ck_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pay_supplier ON payments(supplier_id);
CREATE INDEX idx_pay_shop     ON payments(shop_id);
CREATE INDEX idx_pay_status   ON payments(status);
CREATE INDEX idx_pay_created  ON payments(created_at);
CREATE INDEX idx_pay_pair     ON payments(supplier_id, shop_id);

CREATE TABLE payment_events (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT       NOT NULL,
  action     VARCHAR(40)  NOT NULL,
  user_id    BIGINT       NULL,
  timestamp  DATETIME(6)  NOT NULL,
  details    JSON         NULL,
  INDEX idx_pe_payment (payment_id),
  CONSTRAINT fk_pe_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NULL,
  organization_id BIGINT       NULL,
  action          VARCHAR(60)  NOT NULL,
  entity_type     VARCHAR(60)  NULL,
  entity_id       BIGINT       NULL,
  timestamp       DATETIME(6)  NOT NULL,
  details         JSON         NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts     (timestamp),
  INDEX idx_audit_user   (user_id),
  INDEX idx_audit_org    (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
  event_id     CHAR(36)     NOT NULL,
  event_type   VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64)  NOT NULL,
  payload      JSON         NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  processed_at DATETIME(6)  NULL,
  CONSTRAINT uq_outbox_event_id UNIQUE (event_id),
  INDEX idx_outbox_unprocessed (processed_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.4 notification_db

```sql
-- V1__init_notification.sql

CREATE TABLE notifications (
  id                        BIGINT       AUTO_INCREMENT PRIMARY KEY,
  recipient_user_id         BIGINT       NOT NULL,
  recipient_organization_id BIGINT       NULL,
  type                      VARCHAR(40)  NOT NULL,
  title                     VARCHAR(200) NOT NULL,
  message                   VARCHAR(500) NOT NULL,
  reference_type            VARCHAR(30)  NULL,
  reference_id              BIGINT       NULL,
  read_status               VARCHAR(20)  NOT NULL DEFAULT 'UNREAD',
  created_at                DATETIME(6)  NOT NULL,
  read_at                   DATETIME(6)  NULL,
  CONSTRAINT ck_notif_read CHECK (read_status IN ('UNREAD','READ'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notif_user        ON notifications(recipient_user_id, read_status);
CREATE INDEX idx_notif_org         ON notifications(recipient_organization_id);
CREATE INDEX idx_notif_type        ON notifications(type);
CREATE INDEX idx_notif_created     ON notifications(created_at);

CREATE TABLE processed_events (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  event_id   CHAR(36)     NOT NULL,
  event_type VARCHAR(120) NOT NULL,
  processed  BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at DATETIME(6)  NOT NULL,
  CONSTRAINT uq_proc_event_id UNIQUE (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.5 Flyway Migration Naming Convention

```
V{version}__{description}.sql

Examples:
  V1__init_identity.sql
  V2__add_password_reset_token_index.sql
  V3__add_user_avatar_column.sql
```

- Version numbers are sequential integers (no timestamps — deterministic ordering).
- Undo migrations use `U{version}__{description}.sql` (Flyway Teams/Enterprise).
- Each service has its own `db/migration/{service}/` directory.

### 2.6 Tenant Key Strategy

| Table | Tenant Key | Isolation Level |
|---|---|---|
| `organizations` | None (global) | All visible |
| `users` | `organization_id` (nullable for SYSTEM_ADMIN) | Per-org |
| `products` | `supplier_id` | Per-supplier |
| `orders` | `supplier_id` + `shop_id` | Per-pair |
| `payments` | `supplier_id` + `shop_id` | Per-pair |
| `balance_ledger` | `supplier_id` + `shop_id` | Per-pair |
| `notifications` | `recipient_organization_id` (nullable) | Per-org |

Isolation is enforced at the application layer (repository queries always include tenant filters). SYSTEM_ADMIN bypasses all filters.

---

## 3. API DESIGN

### 3.1 Error Response Envelope

```json
{
  "timestamp": "2026-08-26T10:30:00.000Z",
  "status": 409,
  "error": "CONFLICT",
  "message": "Payment already confirmed",
  "path": "/api/payments/42/confirm",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 3.2 Pagination Response Envelope

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

Query params: `?page=0&size=20&sort=createdAt,desc`

### 3.3 API Endpoints (48 total)

#### Auth — Identity Service (3 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 1 | `POST` | `/api/auth/login` | public | Login → `{accessToken, tokenType, expiresIn, user}` |
| 2 | `POST` | `/api/auth/refresh` | public | Refresh token |
| 3 | `GET` | `/api/auth/me` | authenticated | Current user profile + roles + effective status |

**LoginRequest DTO:**
```json
{ "username": "string", "password": "string" }
```
**LoginResponse DTO:**
```json
{
  "accessToken": "eyJhbG...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "id": 1, "username": "john", "email": "j@x.com",
    "firstName": "John", "lastName": "Doe",
    "roles": ["SHOP_ADMIN"], "organizationId": 100,
    "organizationName": "Acme Shop"
  }
}
```

#### Users — Identity Service (8 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 4 | `GET` | `/api/users` | SYSTEM_ADMIN | List users (filters: org, role, status) — paginated |
| 5 | `GET` | `/api/users/{id}` | SYSTEM_ADMIN, same org admin | User detail |
| 6 | `GET` | `/api/users/{id}/effective-status` | authenticated | Compute effective status (user + org) |
| 7 | `PATCH` | `/api/users/{id}/activate` | SYSTEM_ADMIN, same org admin | Activate user |
| 8 | `PATCH` | `/api/users/{id}/disable` | SYSTEM_ADMIN, same org admin | Disable user |

**UserResponse DTO:**
```json
{
  "id": 1, "username": "john", "email": "j@x.com",
  "firstName": "John", "lastName": "Doe", "phone": "+33...",
  "organizationId": 100, "organizationName": "Acme Shop",
  "roles": ["SHOP_ADMIN"], "status": "ACTIVE",
  "createdAt": "2026-01-15T10:00:00Z"
}
```

#### Supplier Agents — Identity Service (5 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 9 | `GET` | `/api/suppliers/{supplierId}/agents` | SUPPLIER_ADMIN (org scope) | List agents for supplier |
| 10 | `POST` | `/api/suppliers/{supplierId}/agents` | SUPPLIER_ADMIN (org scope) | Create agent |
| 11 | `PATCH` | `/api/suppliers/{supplierId}/agents/{agentId}` | SUPPLIER_ADMIN (org scope) | Update agent |
| 12 | `PATCH` | `/api/suppliers/{supplierId}/agents/{agentId}/activate` | SUPPLIER_ADMIN (org scope) | Activate agent |
| 13 | `PATCH` | `/api/suppliers/{supplierId}/agents/{agentId}/disable` | SUPPLIER_ADMIN (org scope) | Disable agent |

**CreateAgentRequest DTO:**
```json
{
  "firstName": "string", "lastName": "string",
  "email": "string", "phone": "string",
  "username": "string", "role": "SUPPLIER_AGENT"
}
```

#### Suppliers (Admin) — Organization Service (5 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 14 | `GET` | `/api/admin/suppliers` | SYSTEM_ADMIN | List suppliers + stats |
| 15 | `POST` | `/api/admin/suppliers` | SYSTEM_ADMIN | Create supplier + admin account |
| 16 | `GET` | `/api/admin/suppliers/{id}` | SYSTEM_ADMIN | Supplier detail |
| 17 | `PATCH` | `/api/admin/suppliers/{id}` | SYSTEM_ADMIN | Update supplier |
| 18 | `PATCH` | `/api/admin/suppliers/{id}/activate` | SYSTEM_ADMIN | Activate supplier |
| 19 | `PATCH` | `/api/admin/suppliers/{id}/disable` | SYSTEM_ADMIN | Disable supplier + cascade |

**CreateSupplierRequest DTO:**
```json
{
  "name": "string",
  "adminFirstName": "string", "adminLastName": "string",
  "adminEmail": "string", "adminUsername": "string"
}
```

#### Shops (Admin) — Organization Service (5 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 20 | `GET` | `/api/admin/shops` | SYSTEM_ADMIN | List shops + stats |
| 21 | `POST` | `/api/admin/shops` | SYSTEM_ADMIN | Create shop + admin account |
| 22 | `GET` | `/api/admin/shops/{id}` | SYSTEM_ADMIN | Shop detail |
| 23 | `PATCH` | `/api/admin/shops/{id}/activate` | SYSTEM_ADMIN | Activate shop |
| 24 | `PATCH` | `/api/admin/shops/{id}/disable` | SYSTEM_ADMIN | Disable shop + cascade |

#### Supplier/Shop Relations — Organization Service (4 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 25 | `GET` | `/api/shops` | SHOP_ADMIN/SHOP_AGENT | My shop(s) |
| 26 | `GET` | `/api/suppliers` | SUPPLIER_ADMIN/SHOP_ADMIN | Suppliers linked to me |
| 27 | `GET` | `/api/suppliers/{id}/shops` | SUPPLIER_ADMIN | Shops linked to my supplier |
| 28 | `POST` | `/api/suppliers/{supplierId}/shops/{shopId}/link` | SYSTEM_ADMIN | Create relation (idempotent) |
| 29 | `DELETE` | `/api/suppliers/{supplierId}/shops/{shopId}/link` | SYSTEM_ADMIN | Remove relation |

#### Catalog (Products) — Organization Service (6 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 30 | `GET` | `/api/suppliers/{supplierId}/products` | SUPPLIER (org scope) | List products — paginated, filterable by category/status |
| 31 | `POST` | `/api/suppliers/{supplierId}/products` | SUPPLIER_ADMIN | Create product |
| 32 | `PUT` | `/api/suppliers/{supplierId}/products/{productId}` | SUPPLIER_ADMIN | Update product |
| 33 | `PATCH` | `/api/suppliers/{supplierId}/products/{productId}/activate` | SUPPLIER_ADMIN | Activate product |
| 34 | `PATCH` | `/api/suppliers/{supplierId}/products/{productId}/disable` | SUPPLIER_ADMIN | Disable product |
| 35 | `GET` | `/api/suppliers/{supplierId}/products/{productId}/stock` | SUPPLIER (org scope) | Stock movements — paginated |

**CreateProductRequest DTO:**
```json
{
  "sku": "string", "name": "string", "description": "string",
  "categoryId": 1, "familyId": 2, "subfamilyId": 3,
  "unitPrice": 25.50, "currency": "EUR", "unit": "piece",
  "initialQuantity": 100
}
```

#### Orders — Organization Service (6 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 36 | `GET` | `/api/orders` | SUPPLIER/SHOP (org scope) | List orders — paginated, filterable by status, supplier, shop, date range |
| 37 | `POST` | `/api/orders` | SHOP_ADMIN/SHOP_AGENT | Create order |
| 38 | `GET` | `/api/orders/{id}` | actors of the order | Order detail + items + events |
| 39 | `PATCH` | `/api/orders/{id}/confirm` | SUPPLIER_ADMIN | PENDING → CONFIRMED |
| 40 | `PATCH` | `/api/orders/{id}/deliver` | SUPPLIER_AGENT | CONFIRMED → DELIVERED |
| 41 | `PATCH` | `/api/orders/{id}/accept` | SHOP_ADMIN | DELIVERED → ACCEPTED |

**CreateOrderRequest DTO:**
```json
{
  "supplierId": 1, "shopId": 200, "currency": "EUR",
  "items": [
    { "productId": 10, "sku": "WIDGET-01", "quantity": 50, "unitPrice": 25.50 }
  ],
  "note": "string"
}
```

#### Balance — Organization Service (2 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 42 | `GET` | `/api/balance/{supplierId}/{shopId}` | SUPPLIER/SHOP (org scope) | Current balance for pair |
| 43 | `GET` | `/api/balance/{supplierId}/{shopId}/ledger` | SUPPLIER/SHOP (org scope) | Ledger entries — paginated |

#### Payments — Payment Service (6 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 44 | `GET` | `/api/payments` | all authenticated | List — paginated, filtered by org scope, filterable by status, supplier, shop, date |
| 45 | `POST` | `/api/payments` | SHOP_ADMIN/SHOP_AGENT | Create payment → PENDING |
| 46 | `GET` | `/api/payments/{id}` | actors of the payment | Detail + history |
| 47 | `POST` | `/api/payments/{id}/confirm` | SUPPLIER_ADMIN/SUPPLIER_AGENT | PENDING → CONFIRMED |
| 48 | `POST` | `/api/payments/{id}/reject` | SUPPLIER_ADMIN/SUPPLIER_AGENT | PENDING → REJECTED |
| 49 | `POST` | `/api/payments/{id}/cancel` | SHOP_ADMIN/SHOP_AGENT | PENDING → CANCELLED |

**CreatePaymentRequest DTO:**
```json
{
  "supplierId": 1, "amount": 500.00, "currency": "EUR",
  "reference": "INV-2026-001", "comment": "Payment for order..."
}
```

**PaymentResponse DTO:**
```json
{
  "id": 42, "reference": "INV-2026-001",
  "shopId": 200, "shopName": "Acme Shop",
  "supplierId": 1, "supplierName": "Global Supplies",
  "amount": 500.00, "currency": "EUR",
  "status": "PENDING", "rejectionReason": null,
  "createdBy": 5, "createdByName": "John Doe",
  "confirmedBy": null, "confirmedByName": null,
  "createdAt": "2026-08-26T10:30:00Z",
  "updatedAt": "2026-08-26T10:30:00Z"
}
```

#### Notifications — Notification Service (4 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 50 | `GET` | `/api/notifications` | all authenticated | My notifications — paginated, filterable by readStatus |
| 51 | `GET` | `/api/notifications/unread-count` | all authenticated | Count of unread notifications |
| 52 | `PATCH` | `/api/notifications/{id}/read` | owner | Mark as read |
| 53 | `GET` | `/api/notifications/stream` | authenticated (SSE) | Real-time notification stream |

#### Admin (Cross-service) (3 endpoints)

| # | Method | Path | Auth | Description |
|---|---|---|---|---|
| 54 | `GET` | `/api/admin/audit` | SYSTEM_ADMIN | Audit logs — paginated, filterable by action, user, org, date |
| 55 | `GET` | `/api/admin/stats` | SYSTEM_ADMIN | Global stats (orgs, users, payments by status) |
| 56 | `GET` | `/api/admin/payments/stats` | SYSTEM_ADMIN | Payment stats (by status, by supplier, by shop, by period) |

### 3.4 Pagination Strategy

- Default page size: 20, max: 100
- Request: `?page=0&size=20&sort=createdAt,desc`
- Response: `{ content, page, size, totalElements, totalPages }`
- All list endpoints support `page`, `size`, `sort` query params
- Cursor-based pagination for SSE notification stream (not REST)

### 3.5 Idempotency

- `POST /api/payments` accepts `Idempotency-Key` header (UUID)
- Server stores key + response in a TTL-based cache (Redis or in-memory)
- Duplicate key returns cached response (200 OK, not 201)

### 3.6 Concurrency Control

- All update endpoints use `@Version` (optimistic locking via JPA)
- Concurrent update → `409 CONFLICT`
- Response body: `{ "status": 409, "error": "CONFLICT", "message": "Resource was modified by another request" }`

---

## 4. EVENT ARCHITECTURE

### 4.1 Domain Events — Full Payload Specification

Every event extends `DomainEvent` (shared-lib):

```java
public abstract class DomainEvent {
    private final UUID eventId;
    private final String eventType;
    private final String eventVersion;
    private final Instant occurredAt;
    private final String aggregateId;
}
```

| Event | Type | Payload Fields |
|---|---|---|
| `PaymentCreatedEvent` | `payment.created` | `paymentId`, `reference`, `shopId`, `shopName`, `supplierId`, `supplierName`, `amount`, `currency`, `createdBy`, `createdByName` |
| `PaymentConfirmedEvent` | `payment.confirmed` | `paymentId`, `reference`, `shopId`, `supplierId`, `confirmedBy`, `confirmedByName`, `amount`, `currency` |
| `PaymentRejectedEvent` | `payment.rejected` | `paymentId`, `reference`, `shopId`, `supplierId`, `rejectedBy`, `rejectedByName`, `rejectionReason`, `amount`, `currency` |
| `PaymentCancelledEvent` | `payment.cancelled` | `paymentId`, `reference`, `shopId`, `supplierId`, `cancelledBy`, `cancelledByName`, `amount`, `currency` |
| `SupplierCreatedEvent` | `organization.supplier.created` | `organizationId`, `name` |
| `SupplierActivatedEvent` | `organization.supplier.activated` | `organizationId` |
| `SupplierDisabledEvent` | `organization.supplier.disabled` | `organizationId`, `disabledBy` |
| `ShopCreatedEvent` | `organization.shop.created` | `organizationId`, `name` |
| `ShopActivatedEvent` | `organization.shop.activated` | `organizationId` |
| `ShopDisabledEvent` | `organization.shop.disabled` | `organizationId`, `disabledBy` |
| `UserCreatedEvent` | `identity.user.created` | `userId`, `username`, `organizationId`, `roles` |
| `UserActivatedEvent` | `identity.user.activated` | `userId`, `organizationId` |
| `UserDisabledEvent` | `identity.user.disabled` | `userId`, `organizationId`, `disabledBy` |

### 4.2 RabbitMQ Exchange/Queue Topology

```
Exchange: amq.topic (default)

EXCHANGE: payment.events (topic)
  ├── Routing: payment.created      → Queue: notification.payments
  ├── Routing: payment.confirmed    → Queue: notification.payments
  ├── Routing: payment.rejected     → Queue: notification.payments
  └── Routing: payment.cancelled    → Queue: notification.payments

EXCHANGE: organization.events (topic)
  ├── Routing: organization.supplier.created    → Queue: notification.orgs
  ├── Routing: organization.supplier.activated  → Queue: notification.orgs
  ├── Routing: organization.supplier.disabled   → Queue: identity.org-status
  │                                              → Queue: notification.orgs
  ├── Routing: organization.shop.created        → Queue: notification.orgs
  ├── Routing: organization.shop.activated      → Queue: notification.orgs
  └── Routing: organization.shop.disabled       → Queue: identity.org-status
                                               → Queue: notification.orgs

EXCHANGE: identity.events (topic)
  ├── Routing: identity.user.created    → Queue: notification.users
  ├── Routing: identity.user.activated  → Queue: notification.users
  └── Routing: identity.user.disabled   → Queue: notification.users

Queue bindings (summary):
  Queue                      │ Bindings
  ───────────────────────────┼─────────────────────────────────
  notification.payments      │ payment.created, payment.confirmed,
                             │ payment.rejected, payment.cancelled
  notification.orgs          │ organization.supplier.*,
                             │ organization.shop.*
  notification.users         │ identity.user.*
  identity.org-status        │ organization.supplier.disabled,
                             │ organization.shop.disabled
```

**Queue configuration:**
- Durable queues (survive broker restart)
- Prefetch count: 10 (for controlled concurrency)
- Message TTL: 7 days (dead-letter after expiry)
- Dead-letter exchange: `dlx.{exchange-name}` → queue `dlq.{queue-name}`

### 4.3 Outbox Pattern Implementation

```
┌─────────────────────── transaction locale ───────────────────────┐
│  {service}_db                                                      │
│    ├── INSERT into business table (e.g., payments)                 │
│    └── INSERT into outbox_events (event payload JSON)              │
└───────────────────────────────────────────────────────────────────┘
                        │
                        ▼  @Scheduled(polling-interval=1s)
              OutboxRelay (shared-lib)
                ├── SELECT * FROM outbox_events
                │   WHERE processed_at IS NULL
                │   ORDER BY id ASC
                │   LIMIT 100
                ├── For each row:
                │   ├── Convert payload → DomainEvent
                │   ├── Publish to RabbitMQ exchange
                │   └── UPDATE processed_at = NOW()
                └── On failure: skip (retry next tick)
```

**OutboxRelay configuration** (application.yml):
```yaml
outbox:
  polling-interval: 1000     # ms
  batch-size: 100
  max-retries: 5
  retry-delay: 2000          # ms
  exchange-name: ${service}.events
```

**Critical events** (must go through outbox):
- All `Payment*Event`
- `SupplierDisabledEvent`, `ShopDisabledEvent` (cascade guarantee)
- `UserDisabledEvent`

**Non-critical events** (can be published directly but still idempotent on consumer):
- `SupplierCreatedEvent`, `ShopCreatedEvent` (informational)
- `UserCreatedEvent`

### 4.4 Event Sequencing

Events are ordered per aggregate (payment, organization) by the `id` column in `outbox_events` (auto-increment). Consumers process in order. The `eventVersion` field enables forward-compatible schema evolution.

### 4.5 Consumer Idempotency

```java
// In notification-service
@RabbitListener(queues = "notification.payments")
public void handlePaymentEvent(PaymentCreatedEvent event) {
    if (processedEventRepository.existsByEventId(event.getEventId())) {
        return; // already processed
    }
    // ... create notification ...
    processedEventRepository.save(new ProcessedEventEntity(event.getEventId(), event.getEventType()));
}
```

---

## 5. SECURITY ARCHITECTURE

### 5.1 JWT Token Structure

**Header:**
```json
{ "alg": "HS256", "typ": "JWT" }
```

**Payload:**
```json
{
  "sub": "5",
  "username": "john.doe",
  "roles": ["SHOP_ADMIN"],
  "permissions": ["SHOP_CREATE_PAYMENTS", "SHOP_CANCEL_PAYMENTS", "VIEW_PAYMENTS", "VIEW_NOTIFICATIONS"],
  "organizationId": 200,
  "organizationName": "Acme Shop",
  "organizationType": "SHOP",
  "iat": 1724678400,
  "exp": 1724680200,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Signing:** HS256, secret from `JWT_SECRET` env var (≥32 bytes). Production migration path: RS256 with rotating keys.

**Token lifetime:** 30 minutes (configurable via `JWT_EXPIRATION_MINUTES`).

### 5.2 RBAC Permissions Matrix

| Permission | SYSTEM_ADMIN | SUPPLIER_ADMIN | SUPPLIER_AGENT | SHOP_ADMIN | SHOP_AGENT |
|---|:-:|:-:|:-:|:-:|:-:|
| `ADMIN_MANAGE_ORGANIZATIONS` | ✔ | | | | |
| `ADMIN_MANAGE_USERS` | ✔ | | | | |
| `ADMIN_VIEW_AUDIT` | ✔ | | | | |
| `ADMIN_VIEW_STATS` | ✔ | | | | |
| `SUPPLIER_MANAGE_AGENTS` | | ✔ | | | |
| `SUPPLIER_MANAGE_PAYMENTS` | | ✔ | ✔ | | |
| `SUPPLIER_MANAGE_CATALOG` | | ✔ | | | |
| `SUPPLIER_MANAGE_ORDERS` | | ✔ | ✔ | | |
| `SHOP_MANAGE_AGENTS` | | | | ✔ | |
| `SHOP_CREATE_PAYMENTS` | | | | ✔ | ✔ |
| `SHOP_CANCEL_PAYMENTS` | | | | ✔ | ✔ |
| `SHOP_MANAGE_ORDERS` | | | | ✔ | ✔ |
| `VIEW_PAYMENTS` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `VIEW_NOTIFICATIONS` | ✔ | ✔ | ✔ | ✔ | ✔ |

### 5.3 Organization-Level Authorization

```java
// Every service endpoint enforces this pattern:

@PreAuthorize("hasAuthority('SHOP_CREATE_PAYMENTS')")
public PaymentResponse createPayment(@RequestBody CreatePaymentRequest req) {
    AuthenticatedUser user = currentUser.get();

    // 1. Scope check: user's organizationId must match the shopId
    if (!user.getOrganizationId().equals(req.getShopId()) &&
        !user.getRoles().contains("SYSTEM_ADMIN")) {
        throw new ForbiddenException("Cannot create payment for another organization");
    }

    // 2. Resource-level check (for confirm/reject/cancel)
    //    Verify the payment belongs to the user's organization scope
}
```

**Scope enforcement per endpoint type:**

| Endpoint Type | Scope Rule |
|---|---|
| List payments | SYSTEM_ADMIN: all; others: filtered by `shop_id` or `supplier_id` from JWT |
| Create payment | `shopId` in request must equal JWT `organizationId` (SHOP_* roles) |
| Confirm/reject payment | `supplierId` on payment must equal JWT `organizationId` (SUPPLIER_* roles) |
| Cancel payment | `shopId` on payment must equal JWT `organizationId` (SHOP_* roles) |
| Manage agents | `supplierId` in URL must equal JWT `organizationId` (SUPPLIER_ADMIN) |
| Manage products | `supplierId` in URL must equal JWT `organizationId` (SUPPLIER_ADMIN) |
| Orders | Filtered by `supplier_id` or `shop_id` matching JWT scope |

### 5.4 API Gateway Security

```yaml
# GatewaySecurityConfig.java
spring:
  cloud:
    gateway:
      routes:
        - id: identity
          uri: http://identity-service:8082
          predicates:
            - Path=/api/auth/**,/api/users/**,/api/suppliers/*/agents/**
        - id: organization
          uri: http://organization-service:8083
          predicates:
            - Path=/api/admin/**,/api/shops,/api/suppliers/**,/api/orders/**,/api/balance/**
        - id: payment
          uri: http://payment-service:8084
          predicates:
            - Path=/api/payments/**
        - id: notification
          uri: http://notification-service:8085
          predicates:
            - Path=/api/notifications/**
```

**Gateway filters (in order):**
1. **CorrelationIdFilter** — generates/propagates `X-Correlation-Id`
2. **JwtValidationFilter** — validates JWT signature + expiry (stateless)
3. **RateLimitFilter** — 120 req/min per IP, 60 req/min per user (bucket4j)
4. **CORS filter** — allow `http://localhost:8080`, `http://localhost:4200`
5. **RequestLoggingFilter** — log method, path, status, duration

**Headers added by Gateway:**
```
X-Correlation-Id: {uuid}
X-Forwarded-For: {client-ip}
```

### 5.5 Internal Service Authentication

Services do NOT trust Gateway headers. Each service:
1. Re-validates JWT signature + expiry independently
2. Re-checks user status in `identity_db` (SELECT + cache 30s)
3. Re-checks org status in `organization_db` (via REST call to Organization Service)
4. Applies RBAC via `@PreAuthorize`
5. Enforces tenant scope

**Inter-service calls** (Payment → Organization for relation validation) use `INTERNAL_SECRET` header for service-to-service auth (shared secret, not JWT).

```java
// InternalAuthGuard.java
@Component
public class InternalAuthGuard implements HandlerInterceptor {
    @Value("${internal.secret}")
    private String expectedSecret;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String secret = req.getHeader("X-Internal-Secret");
        if (!expectedSecret.equals(secret)) {
            res.setStatus(403);
            return false;
        }
        return true;
    }
}
```

### 5.6 Password Security

- BCrypt with cost factor 12
- Minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit
- Password reset tokens: cryptographically random (SecureRandom), 64 chars, 24-hour expiry

### 5.7 Security Headers

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 0
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'
Cache-Control: no-store
Pragma: no-cache
```

---

## 6. DOCKER ARCHITECTURE

### 6.1 docker-compose.yml (Complete)

```yaml
services:

  mysql:
    image: mysql:8.4
    container_name: payment-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - "${MYSQL_EXTERNAL_PORT:-3307}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: "1.0"

  rabbitmq:
    image: rabbitmq:4-management
    container_name: payment-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "${RABBITMQ_AMQP_PORT:-5673}:5672"
      - "${RABBITMQ_MGMT_PORT:-15673}:15672"
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 20s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"

  identity-service:
    build:
      context: ../backend
      dockerfile: Dockerfile
      args:
        SERVICE_NAME: identity-service
    container_name: payment-identity
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: identity_db
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES:-30}
      API_GATEWAY_URL: http://api-gateway:8081
      INTERNAL_SECRET: ${INTERNAL_SECRET}
      SMTP_HOST: mailpit
      SMTP_PORT: 1025
      MAIL_FROM: noreply@payment-platform.local
      FRONTEND_URL: ${FRONTEND_URL:-http://localhost:8080}
      JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    ports:
      - "8082:8082"
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
    restart: unless-stopped

  organization-service:
    build:
      context: ../backend
      dockerfile: Dockerfile
      args:
        SERVICE_NAME: organization-service
    container_name: payment-organization
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: organization_db
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES:-30}
      API_GATEWAY_URL: http://api-gateway:8081
      INTERNAL_SECRET: ${INTERNAL_SECRET}
      JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    ports:
      - "8083:8083"
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
    restart: unless-stopped

  payment-service:
    build:
      context: ../backend
      dockerfile: Dockerfile
      args:
        SERVICE_NAME: payment-service
    container_name: payment-payment
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: payment_db
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES:-30}
      API_GATEWAY_URL: http://api-gateway:8081
      INTERNAL_SECRET: ${INTERNAL_SECRET}
      JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    ports:
      - "8084:8084"
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
    restart: unless-stopped

  notification-service:
    build:
      context: ../backend
      dockerfile: Dockerfile
      args:
        SERVICE_NAME: notification-service
    container_name: payment-notification
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: notification_db
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES:-30}
      API_GATEWAY_URL: http://api-gateway:8081
      JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    ports:
      - "8085:8085"
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8085/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
    restart: unless-stopped

  api-gateway:
    build:
      context: ../backend
      dockerfile: Dockerfile
      args:
        SERVICE_NAME: api-gateway
    container_name: payment-gateway
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES:-30}
      CORS_ALLOWED_ORIGINS: "${CORS_ALLOWED_ORIGINS:-http://localhost:8080}"
      RATE_LIMIT_PER_MINUTE: ${RATE_LIMIT_PER_MINUTE:-120}
      IDENTITY_SERVICE_URL: identity-service
      IDENTITY_SERVICE_PORT: 8082
      ORGANIZATION_SERVICE_URL: organization-service
      ORGANIZATION_SERVICE_PORT: 8083
      PAYMENT_SERVICE_URL: payment-service
      PAYMENT_SERVICE_PORT: 8084
      NOTIFICATION_SERVICE_URL: notification-service
      NOTIFICATION_SERVICE_PORT: 8085
      JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC"
    ports:
      - "${GATEWAY_PORT:-8081}:8081"
    depends_on:
      - identity-service
      - organization-service
      - payment-service
      - notification-service
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
    restart: unless-stopped

  angular:
    build:
      context: ..
      dockerfile: frontend/Dockerfile
    container_name: payment-frontend
    ports:
      - "${FRONTEND_PORT:-8080}:80"
    depends_on:
      - api-gateway
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:80"]
      interval: 15s
      timeout: 5s
      retries: 3
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 128M
          cpus: "0.25"

  adminer:
    image: adminer:latest
    container_name: payment-adminer
    ports:
      - "8086:8080"
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      ADMINER_DEFAULT_SERVER: mysql
      ADMINER_DESIGN: pepa-linha-dark
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 128M

  mailpit:
    image: axllent/mailpit
    container_name: payment-mailpit
    ports:
      - "8025:8025"
      - "1025:1025"
    networks:
      - payment-net
    deploy:
      resources:
        limits:
          memory: 128M

volumes:
  mysql-data:
  rabbitmq-data:

networks:
  payment-net:
    driver: bridge
```

### 6.2 Network Topology

```
                    ┌─────────────────────────────────┐
                    │          payment-net              │
                    │         (bridge driver)           │
                    │                                   │
   Port 8080 ──────┤  angular (nginx)                  │
   Port 8081 ──────┤  api-gateway                      │
   Port 8082 ──────┤  identity-service                 │
   Port 8083 ──────┤  organization-service             │
   Port 8084 ──────┤  payment-service                  │
   Port 8085 ──────┤  notification-service             │
   Port 3307 ──────┤  mysql                            │
   Port 5673 ──────┤  rabbitmq (AMQP)                  │
   Port 15673 ─────┤  rabbitmq (Management)            │
   Port 8086 ──────┤  adminer                          │
   Port 8025 ──────┤  mailpit (UI)                     │
                    └─────────────────────────────────┘
```

- All internal communication uses Docker DNS (service names)
- External ports are for development only (production uses internal networking only)
- No service exposes ports to host except nginx (8080), gateway (8081), and dev tools

### 6.3 Environment Variables (.env)

```bash
# JWT
JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-bytes-0123456789abcdef
JWT_EXPIRATION_MINUTES=30

# MySQL
MYSQL_ROOT_PASSWORD=root-password-change-me
MYSQL_USER=payment_app
MYSQL_PASSWORD=app-password-change-me

# RabbitMQ
RABBITMQ_USER=payment
RABBITMQ_PASSWORD=rabbit-password-change-me

# Internal service auth
INTERNAL_SECRET=dev-internal-secret-change-me

# Spring
SPRING_PROFILES_ACTIVE=prod

# Frontend
FRONTEND_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:8080

# Ports
GATEWAY_PORT=8081
MYSQL_EXTERNAL_PORT=3307
RABBITMQ_AMQP_PORT=5673
RABBITMQ_MGMT_PORT=15673
FRONTEND_PORT=8080
RATE_LIMIT_PER_MINUTE=120
```

---

## 7. CI/CD PIPELINE

### 7.1 Pipeline Stages

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  BUILD   │───▶│  TEST   │───▶│ SECURITY │───▶│ DOCKER  │───▶│ DEPLOY  │───▶│ VERIFY  │
│          │    │         │    │          │    │         │    │         │    │         │
│ mvn      │    │ unit    │    │ SAST     │    │ build   │    │ stage   │    │ smoke   │
│ compile  │    │ integ   │    │ dep check│    │ push    │    │ promote │    │ tests   │
│ package  │    │ e2e     │    │ secrets  │    │ sign    │    │ prod    │    │ health  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
```

### 7.2 Stage Details

**Stage 1: BUILD**
```bash
mvn clean compile -DskipTests
mvn package -DskipTests
# Output: target/*.jar for each service
```
Gate: compilation success, zero warnings on errorprone.

**Stage 2: TEST**
```bash
# Unit tests (all services)
mvn test

# Integration tests (Testcontainers: MySQL + RabbitMQ)
mvn verify -Pintegration-tests

# Frontend tests
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless

# Code coverage threshold: 80% line coverage
```
Gate: all tests pass, coverage ≥ 80%.

**Stage 3: SECURITY**
```bash
# OWASP Dependency Check
mvn dependency-check:check

# SpotBugs + FindSecBugs
mvn spotbugs:check

# SAST (Semgrep or SonarQube)
semgrep --config auto src/

# Secret scanning (gitleaks)
gitleaks detect --source . --verbose

# Container scanning (Trivy)
trivy image payment-platform/identity-service:latest
```
Gate: zero critical/high CVEs, zero secrets found.

**Stage 4: DOCKER**
```bash
# Build images
docker compose build --no-cache

# Tag with git SHA + semver
docker tag payment-platform/identity-service:latest \
  registry.example.com/payment-platform/identity-service:${GIT_SHA}
docker tag payment-platform/identity-service:latest \
  registry.example.com/payment-platform/identity-service:${SEMVER}

# Push to registry
docker push registry.example.com/payment-platform/identity-service:${GIT_SHA}
docker push registry.example.com/payment-platform/identity-service:${SEMVER}

# Sign images (Cosign)
cosign sign --key cosign.key registry.example.com/payment-platform/identity-service:${SEMVER}
```
Gate: all images build successfully, scan passes.

**Stage 5: DEPLOY (staging)**
```bash
# Deploy to staging environment
kubectl apply -f k8s/staging/
# or
docker compose -f docker-compose.staging.yml up -d

# Run database migrations
flyway -url=jdbc:mysql://mysql:3306/identity_db migrate
flyway -url=jdbc:mysql://mysql:3306/organization_db migrate
flyway -url=jdbc:mysql://mysql:3306/payment_db migrate
flyway -url=jdbc:mysql://mysql:3306/notification_db migrate
```
Gate: all services healthy, smoke tests pass.

**Stage 6: DEPLOY (production)**
```bash
# Blue-green or rolling deploy
kubectl set image deployment/identity-service \
  identity-service=registry.example.com/payment-platform/identity-service:${SEMVER}

# Or Docker Compose for single-node
docker compose -f docker-compose.prod.yml up -d --no-deps identity-service
```

### 7.3 Stage Gates Summary

| Stage | Gate Condition | Failure Action |
|---|---|---|
| BUILD | Compilation success | Block pipeline |
| TEST | All tests pass, coverage ≥ 80% | Block pipeline |
| SECURITY | Zero critical/high CVEs | Block pipeline |
| DOCKER | All images build, scan clean | Block pipeline |
| DEPLOY (staging) | Health checks pass, smoke tests pass | Block production deploy |
| DEPLOY (prod) | Health checks pass | Auto-rollback |

### 7.4 Rollback Strategy

**Automatic rollback:**
- If health check fails within 5 minutes of deploy → automatic rollback to previous version
- Database migrations are additive-only (never destructive in production)

**Manual rollback:**
```bash
# Docker Compose
git checkout HEAD~1 -- docker-compose.prod.yml
docker compose -f docker-compose.prod.yml up -d

# Kubernetes
kubectl rollout undo deployment/identity-service
```

**Database rollback:**
- Forward-only migrations (Flyway)
- Destructive changes require two releases: (1) add new column/table, (2) remove old in next release
- Rollback script: `U{version}__undo_{description}.sql` (Flyway Teams)

### 7.5 CI/CD Pipeline (GitHub Actions)

```yaml
name: CI/CD Pipeline
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_ROOT_PASSWORD: test
          MYSQL_DATABASE: test_db
        ports: ['3306:3306']
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '26'
          distribution: 'temurin'
          cache: maven
      - run: mvn clean verify -Pintegration-tests
      - uses: codecov/codecov-action@v4

  security:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: mvn dependency-check:check
      - uses: github/codeql-action/analyze@v3

  docker:
    needs: security
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - run: docker compose build
      - run: docker compose push

  deploy-staging:
    needs: docker
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: staging
    steps:
      - run: deploy/staging.sh

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    steps:
      - run: deploy/production.sh
```

---

## 8. MONITORING & OBSERVABILITY

### 8.1 Health Checks

**Spring Boot Actuator endpoints** (exposed on each service):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  health:
    db:
      enabled: true
    rabbit:
      enabled: true
    diskspace:
      enabled: true
  info:
    env:
      enabled: true
```

**Custom health indicators per service:**

| Service | Health Check | Logic |
|---|---|---|
| identity-service | `UserHealthIndicator` | DB reachable + user count > 0 |
| organization-service | `OrganizationHealthIndicator` | DB reachable + org count > 0 |
| payment-service | `PaymentHealthIndicator` | DB reachable + outbox lag < 60s |
| notification-service | `NotificationHealthIndicator` | DB reachable + RabbitMQ connected |
| api-gateway | `GatewayHealthIndicator` | All downstream services reachable |

**Docker health check** (each service):
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:{port}/actuator/health"]
  interval: 15s
  timeout: 5s
  retries: 5
  start_period: 40s
```

### 8.2 Structured Logging

**Log format (JSON, via Logback):**
```json
{
  "timestamp": "2026-08-26T10:30:00.123Z",
  "level": "INFO",
  "service": "payment-service",
  "traceId": "a1b2c3d4e5f67890",
  "spanId": "1234567890abcdef",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": 5,
  "organizationId": 200,
  "logger": "com.paymentplatform.payment.application.usecase.CreatePaymentUseCase",
  "message": "Payment created successfully",
  "paymentId": 42,
  "reference": "INV-2026-001",
  "duration_ms": 125
}
```

**Logback configuration** (`logback-spring.xml`):
```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>correlationId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
      <includeMdcKeyName>organizationId</includeMdcKeyName>
      <fieldNames>
        <timestamp>[ignore]</timestamp>
      </fieldNames>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON" />
  </root>

  <logger name="com.paymentplatform" level="DEBUG" />
  <logger name="org.springframework" level="WARN" />
  <logger name="org.hibernate" level="WARN" />
</configuration>
```

**MDC fields propagated via `CorrelationIdFilter`:**
```
correlationId: X-Correlation-Id header (or generated UUID)
userId: from JWT sub claim
organizationId: from JWT organizationId claim
service: application name (from spring.application.name)
```

### 8.3 Metrics (Prometheus)

**Spring Boot Actuator + Micrometer:**
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.75, 0.95, 0.99
      slo:
        http.server.requests: 100ms,500ms
    tags:
      application: ${spring.application.name}
```

**Custom metrics per service:**

| Metric | Type | Tags | Description |
|---|---|---|---|
| `payment_created_total` | Counter | `supplier_id`, `shop_id`, `currency` | Total payments created |
| `payment_status_total` | Counter | `status`, `supplier_id` | Payments by final status |
| `payment_amount` | Timer | `currency` | Payment amount distribution |
| `payment_processing_duration` | Timer | — | Payment creation latency |
| `outbox_pending_events` | Gauge | `service` | Events pending in outbox |
| `outbox_publish_latency` | Timer | `event_type` | Outbox relay publish time |
| `notification_sent_total` | Counter | `type`, `recipient_role` | Notifications sent |
| `notification_sse_connections` | Gauge | — | Active SSE connections |
| `user_login_total` | Counter | `status`, `method` | Login attempts (success/fail) |
| `organization_status` | Gauge | `org_id`, `type`, `status` | Org status (1=active, 0=disabled) |
| `http_client_requests` | Timer | `service`, `method`, `uri` | Inter-service call latency |

**Prometheus scrape configuration:**
```yaml
scrape_configs:
  - job_name: 'payment-platform'
    scrape_interval: 15s
    static_configs:
      - targets:
          - 'identity-service:8082'
          - 'organization-service:8083'
          - 'payment-service:8084'
          - 'notification-service:8085'
          - 'api-gateway:8081'
    metrics_path: '/actuator/prometheus'
```

### 8.4 Alerting Rules (Prometheus/Grafana)

```yaml
# prometheus-alerts.yml
groups:
  - name: payment-platform
    rules:

      # Service availability
      - alert: ServiceDown
        expr: up{job="payment-platform"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.instance }} is down"

      # High error rate
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m])
          / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Error rate > 5% on {{ $labels.instance }}"

      # High latency
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P95 latency > 1s on {{ $labels.instance }}"

      # Outbox lag
      - alert: OutboxLag
        expr: outbox_pending_events > 100
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Outbox lag > 100 events on {{ $labels.instance }}"

      # Database connection pool exhaustion
      - alert: DbConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "DB connection pool > 90% on {{ $labels.instance }}"

      # RabbitMQ queue depth
      - alert: QueueBacklog
        expr: rabbitmq_queue_messages > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Queue {{ $labels.queue }} has {{ $value }} messages"

      # Dead letter queue
      - alert: DeadLetterQueueNotEmpty
        expr: rabbitmq_queue_messages{queue=~"dlq.*"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Dead letter queue {{ $labels.queue }} has messages"

      # Disk space
      - alert: LowDiskSpace
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes < 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Disk space < 10% on {{ $labels.instance }}"

      # JWT expiration approaching
      - alert: JwtExpirationWarning
        expr: time() - process_start_time_seconds > 86400
        for: 1h
        labels:
          severity: info
        annotations:
          summary: "Service {{ $labels.instance }} running > 24h, consider restart for JWT secret rotation"
```

### 8.5 Grafana Dashboard Panels

| Dashboard | Key Panels |
|---|---|
| **API Gateway** | Request rate, error rate, P50/P95/P99 latency, rate limiter rejections, active connections |
| **Payment Service** | Payments created/min, status distribution, processing latency, outbox lag, optimistic lock conflicts |
| **Organization Service** | Orders created/min, order status distribution, stock levels, balance operations |
| **Identity Service** | Login rate (success/fail), active users, JWT issuance, password reset requests |
| **Notification Service** | Notifications sent/min, SSE connections, unread count distribution |
| **Infrastructure** | MySQL connections, RabbitMQ queue depths, JVM heap/GC, CPU/memory per container |

### 8.6 Distributed Tracing

- **Trace propagation:** W3C Trace Context (`traceparent` header)
- **Implementation:** Micrometer Tracing + Brave (Zipkin) or OpenTelemetry
- **Sampling:** 100% for errors, 10% for successful requests (configurable)
- **Backend:** Jaeger or Zipkin (self-hosted or cloud)

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
  zipkin:
    tracing:
      endpoint: http://jaeger:9411/api/v2/spans
```

---

## Summary: Architecture Decision Records

| ADR | Decision | Rationale |
|---|---|---|
| ADR-01 | Spring Cloud Gateway WebMVC | Synchronous stack simplicity; `spring-cloud-starter-gateway-server-webmvc` |
| ADR-02 | No Service Discovery | 4 services + 1 gateway = static routing via env vars is sufficient |
| ADR-03 | RabbitMQ over Kafka | Moderate volume, simple queues, mature Spring AMQP integration |
| ADR-04 | Outbox Pattern (polling) | Guarantees payment persisted ⇒ event published; no XA; idempotent relay |
| ADR-05 | SSE over WebSocket | Unidirectional (server→client), HTTP/2 compatible, simpler infra |
| ADR-06 | JWT HS256 | Simple, shared secret via env var; migration path to RS256 documented |
| ADR-07 | Service re-validates JWT | Gateway is NOT a trust boundary; each service verifies independently |
| ADR-08 | Shared DB per service (not per tenant) | Multi-tenancy via row-level isolation; reduces operational cost |
| ADR-09 | Additive-only migrations | Zero-downtime deploys; destructive changes require two-release cycle |
| ADR-10 | `DECIMAL(19,4)` for money | Exact precision; no floating-point rounding errors |
