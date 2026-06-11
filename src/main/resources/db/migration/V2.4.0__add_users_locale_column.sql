-- Email localization: per-user language preference (en / he).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS locale VARCHAR(5);
