-- Phase 21: Time Clock — attendance table
-- Run this script manually against PostgreSQL after the app has started once
-- (Hibernate creates the base table via ddl-auto=update; this script adds the
--  partial unique index and updated_at trigger that Hibernate cannot generate.)

-- Base table (Hibernate creates this; included here for Flyway / clean-install use)
CREATE TABLE IF NOT EXISTS attendance (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT                   NOT NULL,
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    end_time         TIMESTAMP WITH TIME ZONE,
    duration_seconds BIGINT,
    note             TEXT,
    source           VARCHAR(32)              NOT NULL DEFAULT 'MANUAL',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_attendance_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT chk_end_after_start
        CHECK (end_time IS NULL OR end_time > start_time),

    CONSTRAINT chk_duration_positive
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

-- Regular indexes (Hibernate can also create these via @Index, but included for completeness)
CREATE INDEX IF NOT EXISTS idx_attendance_user_start
    ON attendance (user_id, start_time);

CREATE INDEX IF NOT EXISTS idx_attendance_start_time
    ON attendance (start_time);

-- PARTIAL unique index — Hibernate CANNOT generate this; must be applied manually.
-- Enforces at most one open session per user at the database level.
-- A second INSERT with end_time IS NULL for the same user_id raises a
-- unique-constraint violation (DataIntegrityViolationException in Spring).
CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_active_session
    ON attendance (user_id)
    WHERE end_time IS NULL;

-- Trigger function to auto-update updated_at on every UPDATE.
-- Hibernate's @PreUpdate handles this in Java, but the trigger provides
-- a safety net for any direct SQL updates outside the application.
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS attendance_set_updated_at ON attendance;

CREATE TRIGGER attendance_set_updated_at
    BEFORE UPDATE ON attendance
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();
