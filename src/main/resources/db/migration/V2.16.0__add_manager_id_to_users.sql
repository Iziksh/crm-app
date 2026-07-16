-- Direct-manager self-reference for RBAC scoping (attendance approvals, team visibility).
-- Nullable, additive — ON DELETE SET NULL so removing a manager account never blocks deleting them.
ALTER TABLE users ADD COLUMN manager_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL;
