-- V3 : Ajout role SALES et gestion du stock

-- Seed : role SALES
INSERT INTO roles (code, name) VALUES
  ('SALES', 'Commercial');

-- Seed : permission SALES_MANAGE_ACCOUNTS
INSERT INTO permissions (code) VALUES
  ('SALES_MANAGE_ACCOUNTS');

-- Mapping SALES → permissions
INSERT INTO role_permissions (role_code, permission_code) VALUES
  ('SALES', 'SALES_MANAGE_ACCOUNTS'),
  ('SALES', 'VIEW_PAYMENTS'),
  ('SALES', 'VIEW_NOTIFICATIONS');

-- Permissions pour la gestion du stock (fournisseurs)
INSERT INTO permissions (code) VALUES
  ('SUPPLIER_MANAGE_PRODUCTS'),
  ('SUPPLIER_MANAGE_STOCK');

INSERT INTO role_permissions (role_code, permission_code) VALUES
  ('SUPPLIER_ADMIN', 'SUPPLIER_MANAGE_PRODUCTS'),
  ('SUPPLIER_ADMIN', 'SUPPLIER_MANAGE_STOCK'),
  ('SUPPLIER_AGENT', 'SUPPLIER_MANAGE_STOCK');
