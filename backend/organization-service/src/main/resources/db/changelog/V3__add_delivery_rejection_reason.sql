--liquibase formatted sql

--changeset platform:3 splitStatements:true

ALTER TABLE orders ADD COLUMN delivery_rejection_reason TEXT NULL AFTER notes;
