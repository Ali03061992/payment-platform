--liquibase formatted sql

--changeset platform:12 splitStatements:true
-- Patente (mention complémentaire facture) sur les organisations.

ALTER TABLE organizations
  ADD COLUMN patente VARCHAR(32) NULL;
