--liquibase formatted sql

--changeset platform:16 splitStatements:true
-- Devis versionnés : demande boutique → chiffrage fournisseur (remises + lignes modifiées).
CREATE TABLE IF NOT EXISTS order_quotes (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    order_reference VARCHAR(64) NOT NULL,
    supplier_id CHAR(36) NOT NULL,
    version INT NOT NULL,
    global_discount DECIMAL(5,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(19,3) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19,3) NOT NULL DEFAULT 0,
    total DECIMAL(19,3) NOT NULL DEFAULT 0,
    stamp_amount DECIMAL(19,3) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    lines_json MEDIUMTEXT NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uq_quote_order_version (order_id, version),
    KEY idx_quote_order (order_id),
    KEY idx_quote_supplier (supplier_id)
);
