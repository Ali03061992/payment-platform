--liquibase formatted sql

--changeset platform:19 splitStatements:true
-- Journal des factures : numérotation séquentielle par fournisseur et par année
-- (FA-AAAA-NNNNNN), une ligne par commande facturée, émission idempotente.
CREATE TABLE invoice_journal (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  invoice_number VARCHAR(32) NOT NULL,
  order_id VARCHAR(36) NOT NULL UNIQUE,
  order_reference VARCHAR(64) NOT NULL,
  supplier_id VARCHAR(36) NOT NULL,
  shop_id VARCHAR(36) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'TND',
  total_ttc DECIMAL(19,4) NOT NULL,
  vat_amount DECIMAL(19,4) NULL,
  issued_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_invoice_journal_number_supplier UNIQUE (invoice_number, supplier_id),
  CONSTRAINT fk_invoice_journal_order FOREIGN KEY (order_id) REFERENCES orders (id),
  INDEX idx_invoice_journal_supplier (supplier_id, issued_at),
  INDEX idx_invoice_journal_shop (shop_id, issued_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--changeset platform:19b splitStatements:true
-- Compteur par fournisseur et année, verrouillé en écriture à l'attribution.
CREATE TABLE invoice_sequence (
  supplier_id VARCHAR(36) NOT NULL,
  seq_year INT NOT NULL,
  last_number INT NOT NULL DEFAULT 0,
  PRIMARY KEY (supplier_id, seq_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
