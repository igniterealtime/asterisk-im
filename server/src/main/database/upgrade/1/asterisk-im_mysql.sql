alter table phoneDevice modify isPrimary int not null;
UPDATE ofVersion SET version=1 WHERE name='asterisk-im';