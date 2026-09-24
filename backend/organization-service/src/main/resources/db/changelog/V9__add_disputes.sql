--liquibase formatted sql

--changeset platform:9 splitStatements:true

CREATE TABLE disputes (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  order_id VARCHAR(36) NOT NULL,
  shop_id VARCHAR(36) NOT NULL,
  supplier_id VARCHAR(36) NOT NULL,
  opened_by VARCHAR(36) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  reason VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_disputes_order FOREIGN KEY (order_id) REFERENCES orders (id),
  INDEX idx_disputes_order (order_id),
  INDEX idx_disputes_shop (shop_id),
  INDEX idx_disputes_supplier (supplier_id),
  INDEX idx_disputes_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dispute_messages (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  dispute_id VARCHAR(36) NOT NULL,
  sender_id VARCHAR(36) NOT NULL,
  sender_role VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  timestamp DATETIME(6) NOT NULL,
  CONSTRAINT fk_dispute_messages_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id),
  INDEX idx_dispute_messages_dispute (dispute_id),
  INDEX idx_dispute_messages_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
