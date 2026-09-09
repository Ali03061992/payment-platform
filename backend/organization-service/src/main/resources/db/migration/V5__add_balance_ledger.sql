-- V5 : Balance ledger (mouvements financiers immuables)

CREATE TABLE balance_ledger (
  id VARCHAR(36) PRIMARY KEY,
  supplier_id VARCHAR(36)        NOT NULL,
  shop_id VARCHAR(36)        NOT NULL,
  type            VARCHAR(30)   NOT NULL,
  amount          DECIMAL(19,4) NOT NULL,
  balance_after   DECIMAL(19,4) NOT NULL,
  order_id VARCHAR(36)        NULL,
  payment_id VARCHAR(36)        NULL,
  reference       VARCHAR(100)  NULL,
  reason          TEXT          NULL,
  created_by VARCHAR(36)        NULL,
  created_at      DATETIME(6)   NOT NULL,
  INDEX idx_balance_supplier (supplier_id),
  INDEX idx_balance_shop (shop_id),
  INDEX idx_balance_order (order_id),
  INDEX idx_balance_payment (payment_id),
  INDEX idx_balance_type (type),
  INDEX idx_balance_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
