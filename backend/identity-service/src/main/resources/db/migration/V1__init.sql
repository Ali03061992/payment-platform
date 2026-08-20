-- Identity Service - schéma initial

CREATE TABLE users (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  username        VARCHAR(50)  NOT NULL UNIQUE,
  email           VARCHAR(255) NOT NULL UNIQUE,
  password_hash   VARCHAR(100) NOT NULL,
  first_name      VARCHAR(100) NOT NULL,
  last_name       VARCHAR(100) NOT NULL,
  phone           VARCHAR(30)  NULL,
  organization_id BIGINT       NULL,
  status          VARCHAR(20)  NOT NULL,
  version         BIGINT       NOT NULL DEFAULT 0,
  created_at      DATETIME(6)  NOT NULL,
  updated_at      DATETIME(6)  NOT NULL,
  INDEX idx_users_org (organization_id),
  INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE roles (
  code VARCHAR(30) PRIMARY KEY,
  name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
  user_id   BIGINT      NOT NULL,
  role_code VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role_code),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
  code VARCHAR(60) PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
  role_code       VARCHAR(30) NOT NULL,
  permission_code VARCHAR(60) NOT NULL,
  PRIMARY KEY (role_code, permission_code),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_code) REFERENCES roles (code),
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_code) REFERENCES permissions (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NULL,
  organization_id BIGINT       NULL,
  action          VARCHAR(60)  NOT NULL,
  entity_id       BIGINT       NULL,
  timestamp       DATETIME(6)  NOT NULL,
  details         TEXT         NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts (timestamp),
  INDEX idx_audit_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id     CHAR(36)     NOT NULL UNIQUE,
  event_type   VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64)  NOT NULL,
  payload      TEXT         NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  processed_at DATETIME(6)  NULL,
  INDEX idx_outbox_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_events (
  event_id     CHAR(36)    PRIMARY KEY,
  processed_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed : rôles
INSERT INTO roles (code, name) VALUES
  ('SYSTEM_ADMIN',  'Administrateur global'),
  ('SUPPLIER_ADMIN','Administrateur fournisseur'),
  ('SUPPLIER_AGENT','Agent fournisseur'),
  ('SHOP_ADMIN',    'Administrateur boutique'),
  ('SHOP_AGENT',    'Agent boutique');

-- Seed : permissions
INSERT INTO permissions (code) VALUES
  ('ADMIN_MANAGE_ORGANIZATIONS'),
  ('ADMIN_MANAGE_USERS'),
  ('ADMIN_VIEW_AUDIT'),
  ('ADMIN_VIEW_STATS'),
  ('SUPPLIER_MANAGE_AGENTS'),
  ('SUPPLIER_MANAGE_PAYMENTS'),
  ('SHOP_MANAGE_AGENTS'),
  ('SHOP_CREATE_PAYMENTS'),
  ('SHOP_CANCEL_PAYMENTS'),
  ('VIEW_PAYMENTS'),
  ('VIEW_NOTIFICATIONS');

-- Seed : mapping rôles → permissions
INSERT INTO role_permissions (role_code, permission_code) VALUES
  ('SYSTEM_ADMIN', 'ADMIN_MANAGE_ORGANIZATIONS'),
  ('SYSTEM_ADMIN', 'ADMIN_MANAGE_USERS'),
  ('SYSTEM_ADMIN', 'ADMIN_VIEW_AUDIT'),
  ('SYSTEM_ADMIN', 'ADMIN_VIEW_STATS'),
  ('SYSTEM_ADMIN', 'VIEW_PAYMENTS'),
  ('SYSTEM_ADMIN', 'VIEW_NOTIFICATIONS'),
  ('SUPPLIER_ADMIN', 'SUPPLIER_MANAGE_AGENTS'),
  ('SUPPLIER_ADMIN', 'SUPPLIER_MANAGE_PAYMENTS'),
  ('SUPPLIER_ADMIN', 'VIEW_PAYMENTS'),
  ('SUPPLIER_ADMIN', 'VIEW_NOTIFICATIONS'),
  ('SUPPLIER_AGENT', 'SUPPLIER_MANAGE_PAYMENTS'),
  ('SUPPLIER_AGENT', 'VIEW_PAYMENTS'),
  ('SUPPLIER_AGENT', 'VIEW_NOTIFICATIONS'),
  ('SHOP_ADMIN', 'SHOP_MANAGE_AGENTS'),
  ('SHOP_ADMIN', 'SHOP_CREATE_PAYMENTS'),
  ('SHOP_ADMIN', 'SHOP_CANCEL_PAYMENTS'),
  ('SHOP_ADMIN', 'VIEW_PAYMENTS'),
  ('SHOP_ADMIN', 'VIEW_NOTIFICATIONS'),
  ('SHOP_AGENT', 'SHOP_CREATE_PAYMENTS'),
  ('SHOP_AGENT', 'SHOP_CANCEL_PAYMENTS'),
  ('SHOP_AGENT', 'VIEW_PAYMENTS'),
  ('SHOP_AGENT', 'VIEW_NOTIFICATIONS');