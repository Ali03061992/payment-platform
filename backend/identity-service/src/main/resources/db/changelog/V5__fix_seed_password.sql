--liquibase formatted sql

--changeset platform:5 splitStatements:true

-- Fix broken BCrypt hashes from V4 (password: Admin@123)
UPDATE users SET password_hash = '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi' WHERE password_hash = '$2a$12$9TeAQ3fusKYnemCXSAtwje7Gv5QeHcgapYuVqCrt84.aSPhxagXkq';
