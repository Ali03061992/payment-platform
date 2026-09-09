-- Notification Service - schéma initial

CREATE TABLE notifications (
  id VARCHAR(36) PRIMARY KEY,
  recipient_user_id VARCHAR(36)       NOT NULL,
  recipient_organization_id VARCHAR(36)       NULL,
  type                      VARCHAR(40)  NOT NULL,
  message                   VARCHAR(500) NOT NULL,
  read_status               VARCHAR(20)  NOT NULL,
  created_at                DATETIME(6)  NOT NULL,
  read_at                   DATETIME(6)  NULL,
  INDEX idx_notif_user (recipient_user_id, read_status),
  INDEX idx_notif_org (recipient_organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_events (
  event_id     CHAR(36)    PRIMARY KEY,
  processed_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;