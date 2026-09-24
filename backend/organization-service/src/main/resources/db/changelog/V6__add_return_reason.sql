--liquibase formatted sql

--changeset platform:6 splitStatements:true

ALTER TABLE orders ADD COLUMN return_reason TEXT NULL AFTER delivery_rejection_reason;
