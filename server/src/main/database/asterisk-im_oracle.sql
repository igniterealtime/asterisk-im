create table phoneServer (
    serverID integer not null,
    serverName varchar2(255) not null unique,
    hostname varchar2(255) not null,
    port integer not null,
    username varchar2(255) not null,
    password varchar2(255) not null,
    primary key(serverID)
);

create table phoneDevice (
   deviceID integer not null,
   device varchar2(255) not null,
   extension varchar2(255) not null,
   callerID varchar2(255),
   isPrimary integer not null,
   userID integer,
   serverID integer not null,
   primary key (deviceID)
);

create table phoneUser (
   userID integer not null,
   username varchar2(255) not null unique,
   primary key (userID)
);
alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;
alter table phoneDevice add constraint pD_serverID_fk foreign key (serverID) references phoneServer;

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 2);
