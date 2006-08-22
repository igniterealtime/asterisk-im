alter table phoneDevice change column isPrimary isPrimary int not null;
UPDATE jiveVersion SET version=1 WHERE name='asterisk-im';