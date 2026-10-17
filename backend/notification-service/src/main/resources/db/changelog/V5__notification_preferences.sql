--liquibase formatted sql

--changeset platform:5 splitStatements:true

CREATE TABLE notification_preferences (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6),
  UNIQUE INDEX idx_notif_pref_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
