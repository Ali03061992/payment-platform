--liquibase formatted sql

--changeset platform:18 splitStatements:true
-- Suppression de la remise globale : seules les remises par produit s'appliquent
-- (montant = PU × Qté × remise% / 100, déduit avant TVA).
-- NOTE MySQL : pas de IF EXISTS sur DROP COLUMN (syntaxe MariaDB/H2 uniquement).
ALTER TABLE orders DROP COLUMN global_discount;
ALTER TABLE supplier_quotes DROP COLUMN global_discount;
