--liquibase formatted sql

--changeset platform:4 splitStatements:true

CREATE TABLE web_push_subscriptions (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  organization_id VARCHAR(36),
  endpoint VARCHAR(768) NOT NULL,
  p256dh VARCHAR(255) NOT NULL,
  auth VARCHAR(255) NOT NULL,
  user_agent VARCHAR(512),
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6),
  UNIQUE INDEX idx_webpush_endpoint (endpoint(255)),
  INDEX idx_webpush_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
