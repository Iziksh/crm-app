-- holidays table schema diverged from the entity before Flyway baseline (V2.5.0).
-- Data is safe to drop — IsraeliHolidayService repopulates the table on every startup.

DROP TABLE IF EXISTS holidays CASCADE;

CREATE TABLE holidays (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    holiday_date DATE         NOT NULL,
    name         VARCHAR(255) NOT NULL,
    holiday_type VARCHAR(32)  NOT NULL DEFAULT 'PUBLIC',
    country      CHAR(2)      NOT NULL DEFAULT 'IL',
    holiday_year SMALLINT     NOT NULL,
    credit_hours NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_holiday_date_name UNIQUE (holiday_date, name, country)
);

CREATE INDEX idx_holidays_year_country ON holidays (holiday_year, country);
CREATE INDEX idx_holidays_date          ON holidays (holiday_date);
