create table phoneServer (
    serverID bigint not null,
    serverName varchar(255) not null unique,
    hostname varchar(255) not null,
    port integer not null,
    username varchar(255) not null,
    password varchar(255) not null,
    constraint phoneServer_pk primary key(serverID)
);

alter table phoneDevice add column serverID bigint not null;

UPDATE jiveVersion SET version=2 WHERE name='asterisk-im';