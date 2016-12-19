create table phoneServer (
    serverID integer not null,
    serverName nvarchar(255) not null unique,
    hostname nvarchar(255) not null,
    port integer not null,
    username nvarchar(255) not null,
    password nvarchar(255) not null,
    primary key(serverID)
);

alter table phoneDevice add column serverID integer not null;

alter table phoneDevice add constraint pD_serverID_fk foreign key (serverID) references phoneServer;

UPDATE jiveVersion SET version=2 WHERE name='asterisk-im';