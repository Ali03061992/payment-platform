--liquibase formatted sql

--changeset platform:2 splitStatements:true

ALTER TABLE payments ADD COLUMN order_id VARCHAR(36) NULL AFTER created_by;
ALTER TABLE payments ADD COLUMN due_date DATE NULL AFTER order_id;
