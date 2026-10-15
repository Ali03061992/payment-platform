--liquibase formatted sql

--changeset platform:17 splitStatements:true
-- Demandes de devis découplées des commandes + devis fournisseurs versionnés.
-- Remplace l'ancien flux (order_quotes lié aux commandes).
DROP TABLE IF EXISTS order_quotes;

CREATE TABLE IF NOT EXISTS quote_requests (
    id CHAR(36) NOT NULL PRIMARY KEY,
    reference VARCHAR(64) NOT NULL UNIQUE,
    supplier_id CHAR(36) NOT NULL,
    supplier_name VARCHAR(100) NULL,
    shop_id CHAR(36) NOT NULL,
    shop_name VARCHAR(100) NULL,
    notes VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    lines_json MEDIUMTEXT NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_quote_request_shop (shop_id),
    KEY idx_quote_request_supplier (supplier_id)
);

CREATE TABLE IF NOT EXISTS supplier_quotes (
    id CHAR(36) NOT NULL PRIMARY KEY,
    request_id CHAR(36) NOT NULL,
    request_reference VARCHAR(64) NOT NULL,
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
    UNIQUE KEY uq_supplier_quote_request_version (request_id, version),
    KEY idx_supplier_quote_request (request_id),
    KEY idx_supplier_quote_supplier (supplier_id)
);
