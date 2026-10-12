--liquibase formatted sql

--changeset platform:pay4 splitStatements:true
-- Certificats de retenue à la source (art. 55 IRPP/IS, plateforme TEJ).
-- Snapshot fiscal à la confirmation du paiement ; référence TEJ en phase 2.

CREATE TABLE withholding_certificates (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  payment_id VARCHAR(36) NOT NULL UNIQUE,
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
  payment_reference VARCHAR(64) NOT NULL,
  payment_date DATE NOT NULL,
  billing_period VARCHAR(16) NULL,
  due_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  tej_reference VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_withholding_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
  INDEX idx_withholding_payment (payment_id),
  INDEX idx_withholding_beneficiary (beneficiary_id),
  INDEX idx_withholding_debtor (debtor_id),
  INDEX idx_withholding_status (status),
  INDEX idx_withholding_due (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
