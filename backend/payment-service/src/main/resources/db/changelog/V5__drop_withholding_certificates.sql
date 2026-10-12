--liquibase formatted sql

--changeset platform:pay5 splitStatements:true
-- Les certificats de retenue sont rattachés aux FACTURES DE COMMANDES
-- (organization-service) et non plus aux paiements : suppression de la table.

DROP TABLE IF EXISTS withholding_certificates;
