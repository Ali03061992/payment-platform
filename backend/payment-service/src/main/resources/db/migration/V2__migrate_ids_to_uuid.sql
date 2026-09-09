-- Migrate IDs from BIGINT to UUID for existing databases
-- For MySQL: ALTER TABLE users MODIFY COLUMN id VARCHAR(36) NOT NULL; etc.
-- For H2: ALTER TABLE users ALTER COLUMN id SET DATA TYPE VARCHAR(36);
-- This file is a placeholder for existing DBs; fresh DBs already use VARCHAR(36) via V1.

-- No-op for H2 test compatibility
SELECT 1;
