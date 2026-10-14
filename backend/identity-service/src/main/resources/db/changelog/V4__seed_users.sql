--liquibase formatted sql

--changeset platform:4 splitStatements:true
-- Seed minimal : UNIQUEMENT system.admin (aucune autre donnée initiale).
-- Tout le reste (organisations, utilisateurs, catalogue) se crée via l'application.
-- Mot de passe initial à rotation immédiate après premier boot.
INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'system.admin', 'admin@system.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'System', 'Admin', '+21600000000', NULL, 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN');
