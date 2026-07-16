-- Free-text company/organization name for a contact, captured even when the employer doesn't yet
-- exist as a formal Account record. Nullable, additive — no backfill needed.
ALTER TABLE contacts ADD COLUMN company VARCHAR(255);
