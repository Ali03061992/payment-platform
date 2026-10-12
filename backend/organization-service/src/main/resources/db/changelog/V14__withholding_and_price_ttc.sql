--liquibase formatted sql

--changeset platform:14 splitStatements:true
-- Certificats de retenue rattachés aux FACTURES DE COMMANDES (art. 55 IRPP/IS, TEJ)
-- + prix TTC produits (TVA incluse, non ajoutée).

CREATE TABLE withholding_certificates (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  order_id VARCHAR(36) NOT NULL UNIQUE,
  number VARCHAR(32) NOT NULL UNIQUE,
  debtor_id VARCHAR(36) NOT NULL,
  debtor_name VARCHAR(100) NOT NULL,
  debtor_matricule VARCHAR(32) NULL,
  debtor_address VARCHAR(255) NULL,
  beneficiary_id VARCHAR(36) NOT NULL,
  beneficiary_name VARCHAR(100) NOT NULL,
  beneficiary_matricule VARCHAR(32) NULL,
  beneficiary_address VARCHAR(255) NULL,
  nature VARCHAR(30) NOT NULL,
  gross_amount DECIMAL(19,4) NOT NULL,
  rate DECIMAL(5,2) NOT NULL,
  withholding_amount DECIMAL(19,4) NOT NULL,
  net_amount DECIMAL(19,4) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'TND',
  order_reference VARCHAR(64) NOT NULL,
  order_date DATE NOT NULL,
  billing_period VARCHAR(16) NULL,
  due_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  tej_reference VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_withholding_order FOREIGN KEY (order_id) REFERENCES orders (id),
  INDEX idx_withholding_order (order_id),
  INDEX idx_withholding_beneficiary (beneficiary_id),
  INDEX idx_withholding_debtor (debtor_id),
  INDEX idx_withholding_status (status),
  INDEX idx_withholding_due (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--changeset platform:14b splitStatements:true

ALTER TABLE products
  ADD COLUMN price_ttc BOOLEAN NOT NULL DEFAULT FALSE;

--changeset platform:14c splitStatements:true

ALTER TABLE order_items
  ADD COLUMN price_ttc BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN ht_unit_price DECIMAL(12,2) NULL;
