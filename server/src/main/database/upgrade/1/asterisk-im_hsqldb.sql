alter table phoneDevice change column isPrimary isPrimary integer not null;
UPDATE jiveVersion SET version=1 WHERE name='asterisk-im';