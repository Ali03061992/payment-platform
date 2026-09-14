--liquibase formatted sql

--changeset platform:1 splitStatements:true

CREATE TABLE organizations (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_orgs_type (type),
  INDEX idx_orgs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_shop_relations (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  supplier_id VARCHAR(36) NOT NULL,
  shop_id VARCHAR(36) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uq_relation (supplier_id, shop_id),
  INDEX idx_relation_supplier (supplier_id),
  INDEX idx_relation_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_categories (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  supplier_id VARCHAR(36) NOT NULL,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uq_cat_supplier_code (supplier_id, code),
  INDEX idx_cat_supplier (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_families (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  supplier_id VARCHAR(36) NOT NULL,
  category_id VARCHAR(36) NULL,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uq_family_supplier_code (supplier_id, code),
  INDEX idx_family_supplier (supplier_id),
  INDEX idx_family_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_subfamilies (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  supplier_id VARCHAR(36) NOT NULL,
  family_id VARCHAR(36) NOT NULL,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uq_subf_supplier_code (supplier_id, code),
  INDEX idx_subf_supplier (supplier_id),
  INDEX idx_subf_family (family_id),
  CONSTRAINT fk_subf_family FOREIGN KEY (family_id) REFERENCES product_families (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE family_categories (
  family_id VARCHAR(36) NOT NULL,
  category_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (family_id, category_id),
  CONSTRAINT fk_fc_family FOREIGN KEY (family_id) REFERENCES product_families (id),
  CONSTRAINT fk_fc_category FOREIGN KEY (category_id) REFERENCES product_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  supplier_id VARCHAR(36) NOT NULL,
  name VARCHAR(200) NOT NULL,
  sku VARCHAR(50) NOT NULL,
  description TEXT NULL,
  category_id VARCHAR(36) NULL,
  family_id VARCHAR(36) NULL,
  subfamily_id VARCHAR(36) NULL,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
  unit VARCHAR(20) NULL DEFAULT 'unite',
  quantity INT NOT NULL DEFAULT 0,
  reserved_qty INT NOT NULL DEFAULT 0,
  min_quantity INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uq_supplier_sku (supplier_id, sku),
  INDEX idx_products_supplier (supplier_id),
  INDEX idx_products_status (status),
  INDEX idx_products_category (category_id),
  INDEX idx_products_family (family_id),
  INDEX idx_products_subfamily (subfamily_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_movements (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  product_id VARCHAR(36) NOT NULL,
  supplier_id VARCHAR(36) NOT NULL,
  type VARCHAR(20) NOT NULL,
  quantity INT NOT NULL,
  reference VARCHAR(100) NULL,
  notes TEXT NULL,
  created_by VARCHAR(100) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products (id),
  INDEX idx_stock_mov_product (product_id),
  INDEX idx_stock_mov_supplier (supplier_id),
  INDEX idx_stock_mov_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  reference VARCHAR(64) NOT NULL UNIQUE,
  supplier_id VARCHAR(36) NOT NULL,
  shop_id VARCHAR(36) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  created_by_role VARCHAR(30) NOT NULL,
  source VARCHAR(10) NOT NULL DEFAULT 'SHOP',
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  subtotal DECIMAL(19,4) NOT NULL DEFAULT 0,
  tax_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
  tax_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
  total DECIMAL(19,4) NOT NULL DEFAULT 0,
  currency CHAR(3) NOT NULL DEFAULT 'TND',
  delivery_agent_id VARCHAR(36) NULL,
  received_by VARCHAR(36) NULL,
  received_at DATETIME(6) NULL,
  delivered_at DATETIME(6) NULL,
  planned_delivery_date DATE NULL,
  confirmed_delivery_date DATE NULL,
  asap_payment BOOLEAN NOT NULL DEFAULT FALSE,
  notes TEXT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_orders_supplier (supplier_id),
  INDEX idx_orders_shop (shop_id),
  INDEX idx_orders_status (status),
  INDEX idx_orders_created (created_at),
  INDEX idx_orders_delivery_agent (delivery_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  order_id VARCHAR(36) NOT NULL,
  product_id VARCHAR(36) NOT NULL,
  product_ref VARCHAR(50) NOT NULL,
  product_name VARCHAR(200) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  discount DECIMAL(12,2) NOT NULL DEFAULT 0,
  line_total DECIMAL(19,4) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
  INDEX idx_order_items_order (order_id),
  INDEX idx_order_items_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_events (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  order_id VARCHAR(36) NOT NULL,
  action VARCHAR(40) NOT NULL,
  user_id VARCHAR(36) NULL,
  timestamp DATETIME(6) NOT NULL,
  details TEXT NULL,
  CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE balance_ledger (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  supplier_id VARCHAR(36) NOT NULL,
  shop_id VARCHAR(36) NOT NULL,
  type VARCHAR(30) NOT NULL,
  amount DECIMAL(19,4) NOT NULL,
  balance_after DECIMAL(19,4) NOT NULL,
  order_id VARCHAR(36) NULL,
  payment_id VARCHAR(36) NULL,
  reference VARCHAR(100) NULL,
  reason TEXT NULL,
  created_by VARCHAR(36) NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_balance_supplier (supplier_id),
  INDEX idx_balance_shop (shop_id),
  INDEX idx_balance_type (type),
  INDEX idx_balance_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  user_id VARCHAR(36) NULL,
  organization_id VARCHAR(36) NULL,
  action VARCHAR(60) NOT NULL,
  entity_id VARCHAR(36) NULL,
  timestamp DATETIME(6) NOT NULL,
  details TEXT NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts (timestamp),
  INDEX idx_audit_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  event_id VARCHAR(36) NOT NULL UNIQUE,
  event_type VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  processed_at DATETIME(6) NULL,
  INDEX idx_outbox_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_events (
  event_id VARCHAR(36) PRIMARY KEY,
  processed_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
