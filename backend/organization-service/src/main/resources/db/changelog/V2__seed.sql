--liquibase formatted sql

--changeset platform:2 splitStatements:true
-- Seed volontairement VIDE : aucune donnée initiale (ni organisations,
-- ni relations, ni catalogue). Tout se crée via l'application (admin/API).
SELECT 1;
