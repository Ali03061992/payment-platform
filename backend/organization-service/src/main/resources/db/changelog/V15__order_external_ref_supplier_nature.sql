--liquibase formatted sql

--changeset platform:15 splitStatements:true
-- Référence électronique externe de la facture (TTN/TEJ + QR en haut à droite).

ALTER TABLE orders
  ADD COLUMN external_reference VARCHAR(64) NULL;

--changeset platform:15b splitStatements:true
-- Nature de retenue par défaut paramétrable par fournisseur.

ALTER TABLE organizations
  ADD COLUMN default_withholding_nature VARCHAR(30) NULL;
