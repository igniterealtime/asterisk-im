alter table phoneDevice modify isPrimary integer not null;
UPDATE jiveVersion SET version=1 WHERE name='asterisk-im';