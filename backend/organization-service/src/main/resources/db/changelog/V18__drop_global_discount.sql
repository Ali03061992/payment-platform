--liquibase formatted sql

--changeset platform:18 splitStatements:true
-- Suppression de la remise globale : seules les remises par produit s'appliquent
-- (montant = PU × Qté × remise% / 100, déduit avant TVA).
ALTER TABLE orders DROP COLUMN IF EXISTS global_discount;
ALTER TABLE supplier_quotes DROP COLUMN IF EXISTS global_discount;
