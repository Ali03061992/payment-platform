--liquibase formatted sql

--changeset platform:7 splitStatements:true

-- OAuth passwordless : provider + subject + email vérifié.
-- MySQL/H2 : un UNIQUE simple accepte plusieurs NULL, suffisant pour
-- provider_subject (liaison optionnelle, unicité quand renseigné).

ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_subject VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uq_users_provider_subject ON users(provider_subject);
