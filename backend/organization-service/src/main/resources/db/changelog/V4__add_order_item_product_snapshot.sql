--liquibase formatted sql

--changeset platform:4 splitStatements:true

ALTER TABLE order_items ADD COLUMN product_snapshot JSON NULL AFTER line_total;
