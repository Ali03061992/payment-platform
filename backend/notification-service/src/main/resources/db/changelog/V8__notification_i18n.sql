--liquibase formatted sql

--changeset platform:8 splitStatements:true

ALTER TABLE notifications ADD COLUMN message_key VARCHAR(80) NULL;
ALTER TABLE notifications ADD COLUMN message_params VARCHAR(500) NULL;
