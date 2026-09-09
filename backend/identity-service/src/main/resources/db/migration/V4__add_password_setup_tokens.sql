-- Add password_setup_tokens table for email-based password setup flow

CREATE TABLE password_setup_tokens (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36)      NOT NULL,
  token      VARCHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(6) NOT NULL,
  used       BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_pst_user (user_id),
  INDEX idx_pst_token (token, used),
  CONSTRAINT fk_pst_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
