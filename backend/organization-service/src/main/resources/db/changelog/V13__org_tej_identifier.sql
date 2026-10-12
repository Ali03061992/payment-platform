--liquibase formatted sql

--changeset platform:13 splitStatements:true
-- Identifiant du compte TEJ (optionnel, dépôt des certificats de retenue).

ALTER TABLE organizations
  ADD COLUMN tej_identifier VARCHAR(64) NULL;
