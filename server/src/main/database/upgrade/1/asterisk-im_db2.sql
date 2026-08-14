alter table phoneDevice alter column isPrimary integer not null;
UPDATE ofVersion SET version=1 WHERE name='asterisk-im';