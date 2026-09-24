--liquibase formatted sql

--changeset platform:4a splitStatements:true

ALTER TABLE orders ADD COLUMN estimated_arrival DATETIME(6) NULL AFTER delivery_rejection_reason;
