--liquibase formatted sql

--changeset platform:5 splitStatements:true

ALTER TABLE products ADD COLUMN image_url VARCHAR(500) NULL AFTER description;
