ALTER TABLE phoneDevice ADD COLUMN isPrimary_2 INTEGER NULL;
UPDATE phoneDevice SET isPrimary_2 = CAST (isPrimary AS INTEGER);
ALTER TABLE phoneDevice DROP COLUMN isPrimary;
ALTER TABLE phoneDevice RENAME COLUMN isPrimary_2 TO isPrimary;
ALTER TABLE phoneDevice ALTER COLUMN isPrimary SET NOT NULL;
