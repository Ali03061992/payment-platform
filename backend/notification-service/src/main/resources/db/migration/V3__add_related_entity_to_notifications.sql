-- Add related entity columns to notifications table

ALTER TABLE notifications
  ADD COLUMN related_entity_id VARCHAR(64) NULL,
  ADD COLUMN related_entity_type VARCHAR(40) NULL;
