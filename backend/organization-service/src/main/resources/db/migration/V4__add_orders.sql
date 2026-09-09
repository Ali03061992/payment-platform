-- V4 : Commandes et lignes de commande

CREATE TABLE orders (
  id VARCHAR(36) PRIMARY KEY,
  reference            VARCHAR(64)    NOT NULL UNIQUE,
  supplier_id VARCHAR(36)         NOT NULL,
  shop_id VARCHAR(36)         NOT NULL,
  created_by VARCHAR(36)         NOT NULL,
  created_by_role      VARCHAR(30)    NOT NULL,
  source               VARCHAR(10)    NOT NULL DEFAULT 'SHOP',
  status               VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
  subtotal             DECIMAL(19,4)  NOT NULL DEFAULT 0,
  tax_rate             DECIMAL(5,2)   NOT NULL DEFAULT 0,
  tax_amount           DECIMAL(19,4)  NOT NULL DEFAULT 0,
  total                DECIMAL(19,4)  NOT NULL DEFAULT 0,
  currency             CHAR(3)        NOT NULL DEFAULT 'TND',
  delivery_agent_id VARCHAR(36)         NULL,
  received_by VARCHAR(36)         NULL,
  received_at          DATETIME(6)    NULL,
  delivered_at         DATETIME(6)    NULL,
  asap_payment         BOOLEAN        NOT NULL DEFAULT FALSE,
  notes                TEXT           NULL,
  version              BIGINT         NOT NULL DEFAULT 0,
  created_at           DATETIME(6)    NOT NULL,
  updated_at           DATETIME(6)    NOT NULL,
  UNIQUE KEY uq_order_ref (reference),
  INDEX idx_orders_supplier (supplier_id),
  INDEX idx_orders_shop (shop_id),
  INDEX idx_orders_status (status),
  INDEX idx_orders_created (created_at),
  INDEX idx_orders_delivery_agent (delivery_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36)         NOT NULL,
  product_id VARCHAR(36)         NOT NULL,
  product_ref     VARCHAR(50)    NOT NULL,
  product_name    VARCHAR(200)   NOT NULL,
  quantity        INT            NOT NULL,
  unit_price      DECIMAL(12,2)  NOT NULL,
  discount        DECIMAL(12,2)  NOT NULL DEFAULT 0,
  line_total      DECIMAL(19,4)  NOT NULL,
  created_at      DATETIME(6)    NOT NULL,
  INDEX idx_order_items_order (order_id),
  INDEX idx_order_items_product (product_id),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_events (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36)      NOT NULL,
  action     VARCHAR(40) NOT NULL,
  user_id VARCHAR(36)      NULL,
  timestamp  DATETIME(6) NOT NULL,
  details    TEXT        NULL,
  INDEX idx_order_events_order (order_id),
  CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
