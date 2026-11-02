--liquibase formatted sql

--changeset platform:20 splitStatements:true

ALTER TABLE disputes ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE disputes ADD COLUMN due_at DATETIME(6) NULL;
ALTER TABLE disputes ADD COLUMN assigned_to VARCHAR(36) NULL;

CREATE INDEX idx_disputes_priority ON disputes (priority);
CREATE INDEX idx_disputes_assigned_to ON disputes (assigned_to);
CREATE INDEX idx_disputes_due_at ON disputes (due_at);

CREATE TABLE dispute_status_history (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  dispute_id VARCHAR(36) NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  changed_by VARCHAR(36) NOT NULL,
  note VARCHAR(255) NULL,
  changed_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_dispute_status_history_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id),
  INDEX idx_dispute_status_history_dispute (dispute_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dispute_attachments (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  dispute_id VARCHAR(36) NOT NULL,
  message_id VARCHAR(36) NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploaded_by VARCHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_dispute_attachments_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id),
  INDEX idx_dispute_attachments_dispute (dispute_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dispute_notes (
  id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
  dispute_id VARCHAR(36) NOT NULL,
  author_id VARCHAR(36) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_dispute_notes_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id),
  INDEX idx_dispute_notes_dispute (dispute_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
