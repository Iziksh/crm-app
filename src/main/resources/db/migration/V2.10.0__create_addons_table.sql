CREATE TABLE addons (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    expiry_date DATE,
    account_id  BIGINT       NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addons_account_id ON addons(account_id);
