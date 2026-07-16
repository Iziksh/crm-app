ALTER TABLE users ADD COLUMN account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL;
CREATE INDEX idx_users_account_id ON users(account_id);
