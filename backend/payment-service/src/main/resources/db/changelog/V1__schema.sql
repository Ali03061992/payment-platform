--liquibase formatted sql

--changeset platform:1 splitStatements:true

CREATE TABLE payments (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  reference VARCHAR(64) NOT NULL UNIQUE,
  shop_id VARCHAR(36) NOT NULL,
  supplier_id VARCHAR(36) NOT NULL,
  currency CHAR(3) NOT NULL,
  amount DECIMAL(19,4) NOT NULL,
  status VARCHAR(20) NOT NULL,
  rejection_reason VARCHAR(500) NULL,
  created_by VARCHAR(36) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_payments_supplier (supplier_id),
  INDEX idx_payments_shop (shop_id),
  INDEX idx_payments_status (status),
  INDEX idx_payments_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_events (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  payment_id VARCHAR(36) NOT NULL,
  action VARCHAR(40) NOT NULL,
  user_id VARCHAR(36) NULL,
  timestamp DATETIME(6) NOT NULL,
  details TEXT NULL,
  CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
  INDEX idx_payment_events_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  user_id VARCHAR(36) NULL,
  organization_id VARCHAR(36) NULL,
  action VARCHAR(60) NOT NULL,
  entity_id VARCHAR(36) NULL,
  timestamp DATETIME(6) NOT NULL,
  details TEXT NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts (timestamp),
  INDEX idx_audit_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  event_id VARCHAR(36) NOT NULL UNIQUE,
  event_type VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  processed_at DATETIME(6) NULL,
  INDEX idx_outbox_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_events (
  event_id VARCHAR(36) PRIMARY KEY,
  processed_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
