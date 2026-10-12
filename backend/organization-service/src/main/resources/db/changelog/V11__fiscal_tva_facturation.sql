--liquibase formatted sql

--changeset platform:11 splitStatements:true
-- Fiscalité tunisienne : TVA par produit (19/13/7/0), FODEC, timbre, MF, régimes sans TVA.
-- Références : Code TVA art. 18 (mentions facture), art. 11 (suspension), LF 2023 (timbre 1 DT).

ALTER TABLE products
  ADD COLUMN vat_rate DECIMAL(5,2) NOT NULL DEFAULT 19,
  ADD COLUMN fodec_applicable BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN vat_exemption_text VARCHAR(255) NULL;

--changeset platform:11b splitStatements:true

ALTER TABLE order_items
  ADD COLUMN vat_rate DECIMAL(5,2) NOT NULL DEFAULT 19,
  ADD COLUMN fodec_applicable BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN fodec_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
  ADD COLUMN vat_base DECIMAL(19,4) NOT NULL DEFAULT 0,
  ADD COLUMN vat_amount DECIMAL(19,4) NOT NULL DEFAULT 0;

--changeset platform:11c splitStatements:true

ALTER TABLE orders
  ADD COLUMN fodec_total DECIMAL(19,4) NOT NULL DEFAULT 0,
  ADD COLUMN stamp_amount DECIMAL(19,4) NOT NULL DEFAULT 1,
  ADD COLUMN vat_breakdown TEXT NULL,
  ADD COLUMN fiscal_mention TEXT NULL,
  ADD COLUMN exemption_reason VARCHAR(255) NULL,
  ADD COLUMN buyer_regime VARCHAR(30) NULL DEFAULT 'ASSUJETTI';

--changeset platform:11d splitStatements:true

ALTER TABLE organizations
  ADD COLUMN matricule_fiscal VARCHAR(32) NULL,
  ADD COLUMN address VARCHAR(255) NULL,
  ADD COLUMN fiscal_regime VARCHAR(30) NOT NULL DEFAULT 'ASSUJETTI',
  ADD COLUMN suspension_attestation_number VARCHAR(64) NULL,
  ADD COLUMN suspension_attestation_date DATE NULL,
  ADD COLUMN suspension_expiry_date DATE NULL;
