--liquibase formatted sql

--changeset platform:8 splitStatements:true

ALTER TABLE orders ADD COLUMN payment_terms VARCHAR(20) NULL AFTER asap_payment;
ALTER TABLE orders ADD COLUMN due_date DATE NULL AFTER payment_terms;
