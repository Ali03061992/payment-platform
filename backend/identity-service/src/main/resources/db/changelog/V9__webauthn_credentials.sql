--liquibase formatted sql

--changeset platform:9 splitStatements:true

-- Passkeys (WebAuthn) : stocke l'identifiant du credential et la clé publique
-- COSE encodés en binaire. Aucune donnée biométrique n'est persistée — la
-- vérification de l'empreinte/Face ID est faite par le SE via l'authenticator.
-- statut ACTIVE|REVOKED : la révocation est logique, l'authentification refuse
-- tout credential non ACTIVE.

CREATE TABLE webauthn_credentials (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  credential_id VARBINARY(1023) NOT NULL,
  public_key VARBINARY(1024) NOT NULL,
  attestation_object VARBINARY(4096) NULL,
  signature_count BIGINT NOT NULL DEFAULT 0,
  label VARCHAR(100) NOT NULL,
  credential_type VARCHAR(32) NOT NULL DEFAULT 'public-key',
  transports VARCHAR(120) NULL,
  uv_initialized BOOLEAN NOT NULL DEFAULT FALSE,
  backup_eligible BOOLEAN NOT NULL DEFAULT FALSE,
  backup_state BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6) NULL,
  CONSTRAINT fk_wac_user FOREIGN KEY (user_id) REFERENCES users (id),
  UNIQUE INDEX idx_wac_credential_id (credential_id),
  INDEX idx_wac_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
