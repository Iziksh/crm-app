-- CHAR(2) (bpchar) does not satisfy Hibernate's varchar(2) validation.
ALTER TABLE holidays ALTER COLUMN country TYPE VARCHAR(2) USING country::VARCHAR;
