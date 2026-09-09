-- Organization Service - schéma initial

CREATE TABLE organizations (
  id VARCHAR(36) PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  type       VARCHAR(20)  NOT NULL,
  status     VARCHAR(20)  NOT NULL,
  version    BIGINT       NOT NULL DEFAULT 0,
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  INDEX idx_orgs_type (type),
  INDEX idx_orgs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_shop_relations (
  id VARCHAR(36) PRIMARY KEY,
  supplier_id VARCHAR(36)      NOT NULL,
  shop_id VARCHAR(36)      NOT NULL,
  status      VARCHAR(20) NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  UNIQUE KEY uq_relation (supplier_id, shop_id),
  INDEX idx_relation_supplier (supplier_id),
  INDEX idx_relation_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36)       NULL,
  organization_id VARCHAR(36)       NULL,
  action          VARCHAR(60)  NOT NULL,
  entity_id VARCHAR(36)       NULL,
  timestamp       DATETIME(6)  NOT NULL,
  details         TEXT         NULL,
  INDEX idx_audit_action (action),
  INDEX idx_audit_ts (timestamp),
  INDEX idx_audit_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
  id VARCHAR(36) PRIMARY KEY,
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