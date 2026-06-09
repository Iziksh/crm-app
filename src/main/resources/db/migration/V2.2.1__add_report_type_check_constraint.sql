-- Phase F: Enforce the closed set of valid report_type values at the DB level.
-- The Java enum AttendanceReportType is the authoritative definition;
-- this constraint provides a safety net for any direct SQL writes.

ALTER TABLE attendance_report
    DROP CONSTRAINT IF EXISTS chk_report_type_valid;

ALTER TABLE attendance_report
    ADD CONSTRAINT chk_report_type_valid
    CHECK (report_type IN (
        'PRESENCE', 'VACATION', 'SICK', 'RESERVE_DUTY', 'HOLIDAY', 'ABSENCE'
    ));