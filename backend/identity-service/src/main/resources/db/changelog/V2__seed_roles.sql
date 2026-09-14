--liquibase formatted sql

--changeset platform:2 splitStatements:true

INSERT INTO roles (code, name) VALUES
  ('SYSTEM_ADMIN', 'Administrateur global'),
  ('SUPPLIER_ADMIN', 'Administrateur fournisseur'),
  ('SUPPLIER_AGENT', 'Agent fournisseur'),
  ('SHOP_ADMIN', 'Administrateur boutique'),
  ('SHOP_AGENT', 'Agent boutique');
