-- Billing & Documents addon (Phase 26): business-profile fields on the tenant (workspace) and
-- customer (account) records, reused instead of introducing new Company/Customer entities.

ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS tax_status VARCHAR(20);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS legal_name VARCHAR(255);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS tax_id VARCHAR(20);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS business_address VARCHAR(500);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS business_email VARCHAR(255);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS business_phone VARCHAR(50);

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS tax_id VARCHAR(20);
