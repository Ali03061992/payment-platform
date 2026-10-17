--liquibase formatted sql

--changeset platform:10 splitStatements:true

-- Challenges WebAuthn éphémères à usage unique (anti-rejeu).
-- payload_json conserve les options exactes émises afin de rejouer la
-- vérification (challenge, RP ID, origine) à la confirmation du credential.
-- Un challenge est consommé à la première tentative valide ou invalide.

CREATE TABLE webauthn_challenges (
  id VARCHAR(36) PRIMARY KEY,
  challenge VARBINARY(256) NOT NULL,
  purpose VARCHAR(20) NOT NULL,
  user_id VARCHAR(36) NULL,
  payload_json TEXT NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_wachallenge_lookup (challenge, purpose, consumed, expires_at),
  INDEX idx_wachallenge_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
