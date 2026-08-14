alter table phoneDevice change column isPrimary isPrimary integer not null;
UPDATE ofVersion SET version=1 WHERE name='asterisk-im';