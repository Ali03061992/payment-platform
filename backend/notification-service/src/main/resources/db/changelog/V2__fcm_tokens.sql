--liquibase formatted sql

--changeset platform:2 splitStatements:true

CREATE TABLE fcm_tokens (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(512) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6) NOT NULL,
  UNIQUE INDEX idx_fcm_token_unique (token),
  INDEX idx_fcm_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
