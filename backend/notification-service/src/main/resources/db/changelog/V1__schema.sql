--liquibase formatted sql

--changeset platform:1 splitStatements:true

CREATE TABLE notifications (
  id VARCHAR(36) PRIMARY KEY,
  recipient_user_id VARCHAR(36) NULL,
  recipient_organization_id VARCHAR(36) NULL,
  type VARCHAR(40) NOT NULL,
  message VARCHAR(500) NOT NULL,
  read_status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  read_at DATETIME(6) NULL,
  related_entity_id VARCHAR(64) NULL,
  related_entity_type VARCHAR(40) NULL,
  INDEX idx_notif_user (recipient_user_id, read_status),
  INDEX idx_notif_org (recipient_organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id VARCHAR(36) PRIMARY KEY,
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
  id VARCHAR(36) PRIMARY KEY,
  event_id CHAR(36) NOT NULL UNIQUE,
  event_type VARCHAR(120) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  processed_at DATETIME(6) NULL,
  INDEX idx_outbox_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_events (
  event_id CHAR(36) PRIMARY KEY,
  processed_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
