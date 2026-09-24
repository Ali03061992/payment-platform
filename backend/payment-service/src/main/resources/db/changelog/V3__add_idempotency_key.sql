--liquibase formatted sql

--changeset platform:3 splitStatements:true

ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(64) NULL;
CREATE UNIQUE INDEX ux_payments_idempotency_key ON payments(idempotency_key);
