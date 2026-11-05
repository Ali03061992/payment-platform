--liquibase formatted sql

--changeset platform:21 splitStatements:true

ALTER TABLE supplier_quotes ADD COLUMN asap_payment BOOLEAN NOT NULL DEFAULT FALSE;
