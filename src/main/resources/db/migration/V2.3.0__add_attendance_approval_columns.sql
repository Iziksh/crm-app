-- Phase F extension: missed clock-in correction + manager approval workflow.
-- NULL approval_status = normal punch session (no approval needed).
-- PENDING / APPROVED / REJECTED apply only to manually-entered corrections.

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS approval_status  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS approved_by      BIGINT,
    ADD COLUMN IF NOT EXISTS approved_at      TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Optional FK: allows DB to enforce referential integrity on approver.
-- Uses IF NOT EXISTS pattern so re-running is idempotent.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_attendance_approved_by'
    ) THEN
        ALTER TABLE attendance
            ADD CONSTRAINT fk_attendance_approved_by
            FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_attendance_approval_status
    ON attendance (approval_status)
    WHERE approval_status IS NOT NULL;