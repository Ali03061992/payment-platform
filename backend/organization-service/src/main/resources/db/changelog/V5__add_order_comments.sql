--liquibase formatted sql

--changeset platform:5a splitStatements:true

CREATE TABLE order_comments (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  order_id VARCHAR(36) NOT NULL,
  author_id VARCHAR(36) NOT NULL,
  author_name VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_order_comments_order FOREIGN KEY (order_id) REFERENCES orders (id),
  INDEX idx_order_comments_order (order_id),
  INDEX idx_order_comments_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
