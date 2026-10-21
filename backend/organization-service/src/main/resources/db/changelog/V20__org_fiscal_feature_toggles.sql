--liquibase formatted sql

--changeset platform:20 splitStatements:true
-- Interrupteurs fiscaux du fournisseur : documents TEIF (TTN) et certificats TEJ.
ALTER TABLE organizations
  ADD COLUMN teif_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN tej_enabled BOOLEAN NOT NULL DEFAULT TRUE;
