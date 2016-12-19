alter table phoneDevice alter column isPrimary integer not null;
UPDATE jiveVersion SET version=1 WHERE name='asterisk-im';