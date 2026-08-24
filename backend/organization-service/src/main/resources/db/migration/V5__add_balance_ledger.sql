-- V5 : Balance ledger (mouvements financiers immuables)

CREATE TABLE balance_ledger (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id     BIGINT        NOT NULL,
  shop_id         BIGINT        NOT NULL,
  type            VARCHAR(30)   NOT NULL,
  amount          DECIMAL(19,4) NOT NULL,
  balance_after   DECIMAL(19,4) NOT NULL,
  order_id        BIGINT        NULL,
  payment_id      BIGINT        NULL,
  reference       VARCHAR(100)  NULL,
  reason          TEXT          NULL,
  created_by      BIGINT        NULL,
  created_at      DATETIME(6)   NOT NULL,
  INDEX idx_balance_supplier (supplier_id),
  INDEX idx_balance_shop (shop_id),
  INDEX idx_balance_order (order_id),
  INDEX idx_balance_payment (payment_id),
  INDEX idx_balance_type (type),
  INDEX idx_balance_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
