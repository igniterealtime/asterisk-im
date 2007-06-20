create table phoneServer (
    serverID integer not null,
    serverName varchar2(255) not null unique,
    hostname varchar2(255) not null,
    port integer not null,
    username varchar2(255) not null,
    password varchar2(255) not null,
    primary key(serverID)
);

alter table phoneDevice add column serverID integer not null;

alter table phoneDevice add constraint pD_serverID_fk foreign key (serverID) references phoneServer;

UPDATE jiveVersion SET version=2 WHERE name='asterisk-im';