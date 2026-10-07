--liquibase formatted sql

--changeset platform:8 splitStatements:true

ALTER TABLE users ADD COLUMN preferred_lang VARCHAR(5) NOT NULL DEFAULT 'fr';
ALTER TABLE users ADD COLUMN accent_color1 VARCHAR(7) NOT NULL DEFAULT '#0284c7';
ALTER TABLE users ADD COLUMN accent_color2 VARCHAR(7) NOT NULL DEFAULT '#e63946';
ALTER TABLE users ADD COLUMN tour_seen BOOLEAN NOT NULL DEFAULT FALSE;
