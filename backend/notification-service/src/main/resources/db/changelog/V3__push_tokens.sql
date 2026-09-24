--liquibase formatted sql

--changeset platform:3 splitStatements:true

CREATE TABLE push_tokens (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  organization_id VARCHAR(36),
  token VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6),
  UNIQUE INDEX idx_push_token_unique (user_id, token),
  INDEX idx_push_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
