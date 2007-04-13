create table phoneServer (
    serverID int8 not null,
    serverName varchar(255) not null unique,
    hostname varchar(255) not null,
    port integer not null,
    username varchar(255) not null,
    password varchar(255) not null,
    primary key(serverID)
);

alter table phoneDevice add column serverID int8 not null;

UPDATE jiveVersion SET version=2 WHERE name='asterisk-im';