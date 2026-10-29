--liquibase formatted sql

--changeset platform:7 splitStatements:true

ALTER TABLE fcm_tokens
  ADD COLUMN organization_id VARCHAR(36) NULL AFTER user_id,
  ADD INDEX idx_fcm_organization (organization_id);
