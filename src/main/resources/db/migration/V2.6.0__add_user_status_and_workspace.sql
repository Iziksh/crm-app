-- Add lifecycle status for the OTP invitation flow
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Primary workspace (company) for tenant-scoped admin operations
ALTER TABLE users ADD COLUMN IF NOT EXISTS workspace_id BIGINT
    REFERENCES workspaces(id) ON DELETE SET NULL;

-- Back-fill: any previously-disabled user gets DISABLED status
UPDATE users SET status = 'DISABLED' WHERE NOT enabled AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_users_workspace_id ON users(workspace_id);
CREATE INDEX IF NOT EXISTS idx_users_status        ON users(status);
