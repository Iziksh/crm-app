-- Work-location/mode classification (office/WFH/field/on-call) and a free-text project/cost-center
-- tag for internal control purposes. Both nullable/additive — no backfill needed.
ALTER TABLE attendance_report ADD COLUMN work_type VARCHAR(32);
ALTER TABLE attendance_report ADD COLUMN project_tag VARCHAR(255);
