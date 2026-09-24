--liquibase formatted sql

--changeset platform:6 splitStatements:true

-- M1 : refresh tokens opaques (rotation + révocation). Seul l'empreinte SHA-256
-- est persistée, jamais le token brut. TTL applicatif (défaut 7 j).

CREATE TABLE refresh_tokens (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  user_id VARCHAR(36) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  replaced_by VARCHAR(64) NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_refresh_tokens_user (user_id),
  INDEX idx_refresh_tokens_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
