-- Phase F: Manual Attendance Reporting — attendance_report table
-- Separate from the Phase 21 attendance (clock sessions) table.
-- Multiple rows per (user_id, report_date) are allowed.

CREATE TABLE IF NOT EXISTS attendance_report (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    report_date        DATE        NOT NULL,
    entry_time         TIME,
    exit_time          TIME,
    duration_minutes   INT         CHECK (duration_minutes IS NULL OR duration_minutes >= 0),
    note               TEXT,
    report_type        VARCHAR(32) NOT NULL DEFAULT 'PRESENCE',
    equate_to_standard BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_attendance_report_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT chk_presence_requires_times
        CHECK (
            report_type != 'PRESENCE'
            OR (entry_time IS NOT NULL AND exit_time IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_ar_user_date
    ON attendance_report (user_id, report_date);

CREATE INDEX IF NOT EXISTS idx_ar_date
    ON attendance_report (report_date);

CREATE INDEX IF NOT EXISTS idx_ar_user_date_type
    ON attendance_report (user_id, report_date, report_type);

-- Reuses the trg_set_updated_at() function created in V2.0.0.
DROP TRIGGER IF EXISTS attendance_report_set_updated_at ON attendance_report;

CREATE TRIGGER attendance_report_set_updated_at
    BEFORE UPDATE ON attendance_report
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();