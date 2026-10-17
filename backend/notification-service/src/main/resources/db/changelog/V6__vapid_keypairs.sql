--liquibase formatted sql

--changeset platform:6 splitStatements:true

CREATE TABLE vapid_keypairs (
  id VARCHAR(36) PRIMARY KEY,
  public_key VARCHAR(255) NOT NULL,
  private_key VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
