alter table phoneDevice modify isPrimary int not null;
UPDATE jiveVersion SET version=1 WHERE name='asterisk-im';