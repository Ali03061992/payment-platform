--liquibase formatted sql

--changeset platform:10 splitStatements:true

-- M3 : soft-delete catalogue — l'historique (produits, commandes) survit aux
-- suppressions. Les lignes restent en base avec deleted_at renseigné.

ALTER TABLE product_categories ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;
ALTER TABLE product_families ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;
