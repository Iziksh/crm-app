-- Phase 21: Time Clock — holidays table
-- Stores Israeli public holidays computed by IsraeliHolidayService (Phase 24).
-- Populated automatically on startup via ApplicationRunner bootstrap bean.

CREATE TABLE IF NOT EXISTS holidays (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date         DATE         NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(32)  NOT NULL DEFAULT 'PUBLIC',
    country      CHAR(2)      NOT NULL DEFAULT 'IL',
    year         SMALLINT     NOT NULL,
    credit_hours NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    -- Prevent duplicate entries for the same holiday on the same date
    CONSTRAINT uq_holiday_date_name
        UNIQUE (date, name, country)
);

-- Year + country lookup (used by annual recalculation and monthly reports)
CREATE INDEX IF NOT EXISTS idx_holidays_year_country
    ON holidays (year, country);

-- Date-range lookup (is today a holiday? monthly report range queries)
CREATE INDEX IF NOT EXISTS idx_holidays_date
    ON holidays (date);
