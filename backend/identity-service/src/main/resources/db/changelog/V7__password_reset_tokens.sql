--liquibase formatted sql

--changeset platform:7 splitStatements:true

-- Mot de passe oublié : tokens de réinitialisation single-use (TTL 30 min
-- par défaut, cf. app.password-reset.token-expiry-minutes). Même modèle que
-- password_setup_tokens : token opaque unique, marqué used après usage.

CREATE TABLE password_reset_tokens (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(6) NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_password_reset_tokens_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
