create table phoneServer (
    serverID bigint not null,
    serverName varchar(255) not null unique,
    hostname varchar(255) not null,
    port int not null,
    username varchar(255) not null,
    password varchar(255) not null,
    primary key(serverID)
);

alter table phoneDevice add column serverID bigint not null;

UPDATE jiveVersion SET version=2 WHERE name='asterisk-im';