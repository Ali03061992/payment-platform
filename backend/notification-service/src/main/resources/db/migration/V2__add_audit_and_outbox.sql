-- Add audit_logs and outbox_events tables missing from V1

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
