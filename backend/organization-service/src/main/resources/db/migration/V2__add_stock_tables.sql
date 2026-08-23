-- V2 : Gestion du stock fournisseur

CREATE TABLE products (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id     BIGINT        NOT NULL,
  name            VARCHAR(200)  NOT NULL,
  sku             VARCHAR(50)   NOT NULL,
  description     TEXT          NULL,
  unit_price      DECIMAL(12,2) NOT NULL DEFAULT 0,
  currency        VARCHAR(3)    NOT NULL DEFAULT 'EUR',
  quantity        INT           NOT NULL DEFAULT 0,
  min_quantity    INT           NOT NULL DEFAULT 0,
  status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  version         BIGINT        NOT NULL DEFAULT 0,
  created_at      DATETIME(6)   NOT NULL,
  updated_at      DATETIME(6)   NOT NULL,
  UNIQUE KEY uq_supplier_sku (supplier_id, sku),
  INDEX idx_products_supplier (supplier_id),
  INDEX idx_products_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_movements (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id      BIGINT        NOT NULL,
  supplier_id     BIGINT        NOT NULL,
  type            VARCHAR(20)   NOT NULL,
  quantity        INT           NOT NULL,
  reference       VARCHAR(100)  NULL,
  notes           TEXT          NULL,
  created_by      VARCHAR(100)  NULL,
  created_at      DATETIME(6)   NOT NULL,
  INDEX idx_stock_mov_product (product_id),
  INDEX idx_stock_mov_supplier (supplier_id),
  INDEX idx_stock_mov_type (type),
  CONSTRAINT fk_stock_mov_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
