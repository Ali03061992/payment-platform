--liquibase formatted sql

--changeset platform:4b splitStatements:true

ALTER TABLE orders ADD COLUMN global_discount DECIMAL(5,2) NOT NULL DEFAULT 0 AFTER tax_amount;
