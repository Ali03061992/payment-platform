--liquibase formatted sql

--changeset platform:8 splitStatements:true

-- L'auth OAuth (Google/Microsoft) résout le compte par email : system.admin
-- doit porter sa vraie adresse. Idempotent (ne touche que l'ancienne valeur).
UPDATE users SET email = 'ali.ben.amor.1992@hotmail.com'
WHERE username = 'system.admin' AND email = 'admin@system.com';
