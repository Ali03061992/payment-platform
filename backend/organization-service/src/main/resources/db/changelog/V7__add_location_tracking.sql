--liquibase formatted sql

--changeset platform:7 splitStatements:true

ALTER TABLE orders ADD COLUMN last_latitude DECIMAL(9,6) NULL AFTER estimated_arrival;
ALTER TABLE orders ADD COLUMN last_longitude DECIMAL(9,6) NULL AFTER last_latitude;
ALTER TABLE orders ADD COLUMN last_location_update DATETIME(6) NULL AFTER last_longitude;
