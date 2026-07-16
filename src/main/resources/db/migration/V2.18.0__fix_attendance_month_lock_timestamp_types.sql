-- V2.17.0 created submitted_at/approved_at as plain TIMESTAMP; the entity maps them as
-- TIMESTAMP WITH TIME ZONE (same convention as the attendance table), causing a Hibernate
-- schema-validation failure on startup. Table is new/empty, so a plain type change is safe.
ALTER TABLE attendance_month_lock ALTER COLUMN submitted_at TYPE TIMESTAMPTZ;
ALTER TABLE attendance_month_lock ALTER COLUMN approved_at TYPE TIMESTAMPTZ;
