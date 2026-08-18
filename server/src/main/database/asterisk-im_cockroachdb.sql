-- CockroachDB is reached through the standard PostgreSQL JDBC driver, so this script follows
-- asterisk-im_postgresql.sql. The two indexes are the difference: CockroachDB does not index the
-- referencing side of a foreign key for you.

create table phoneServer (
    serverID int8 not null,
    serverName varchar(255) not null unique,
    hostname varchar(255) not null,
    port integer not null,
    username varchar(255) not null,
    password varchar(255) not null,
    primary key (serverID)
);

create table phoneDevice (
   deviceID int8 not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerID varchar(255),
   isPrimary integer not null,
   userID int8,
   serverID int8 not null,
   primary key (deviceID)
);

create table phoneUser (
   userID int8 not null,
   username varchar(255) not null unique,
   primary key (userID)
);

create index phoneDevice_userID_idx on phoneDevice (userID);
create index phoneDevice_serverID_idx on phoneDevice (serverID);

alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;
alter table phoneDevice add constraint pD_serverID_fk foreign key (serverID) references phoneServer;

INSERT INTO ofVersion (name, version) VALUES ('asterisk-im', 2);
