-- V3 : Hiérarchie produit (catégories, familles, sous-familles) + reserved_qty

CREATE TABLE product_categories (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT       NOT NULL,
  name        VARCHAR(100) NOT NULL,
  code        VARCHAR(30)  NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  UNIQUE KEY uq_cat_supplier_code (supplier_id, code),
  INDEX idx_cat_supplier (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_families (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT       NOT NULL,
  category_id BIGINT       NOT NULL,
  name        VARCHAR(100) NOT NULL,
  code        VARCHAR(30)  NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  UNIQUE KEY uq_family_supplier_code (supplier_id, code),
  INDEX idx_family_supplier (supplier_id),
  INDEX idx_family_category (category_id),
  CONSTRAINT fk_family_category FOREIGN KEY (category_id) REFERENCES product_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_subfamilies (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT       NOT NULL,
  family_id   BIGINT       NOT NULL,
  name        VARCHAR(100) NOT NULL,
  code        VARCHAR(30)  NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  UNIQUE KEY uq_subf_supplier_code (supplier_id, code),
  INDEX idx_subf_supplier (supplier_id),
  INDEX idx_subf_family (family_id),
  CONSTRAINT fk_subf_family FOREIGN KEY (family_id) REFERENCES product_families (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ajouter reserved_qty et les colonnes de hiérarchie aux produits existants
ALTER TABLE products
  ADD COLUMN reserved_qty   INT NOT NULL DEFAULT 0 AFTER quantity,
  ADD COLUMN category_id    BIGINT NULL AFTER description,
  ADD COLUMN family_id      BIGINT NULL AFTER category_id,
  ADD COLUMN subfamily_id   BIGINT NULL AFTER family_id,
  ADD COLUMN unit           VARCHAR(20) NULL DEFAULT 'unité' AFTER currency,
  ADD INDEX idx_products_category (category_id),
  ADD INDEX idx_products_family (family_id),
  ADD INDEX idx_products_subfamily (subfamily_id);
