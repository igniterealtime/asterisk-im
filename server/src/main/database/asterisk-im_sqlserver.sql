create table phoneServer (
    serverID integer not null,
    serverName nvarchar(255) not null unique,
    hostname nvarchar(255) not null,
    port integer not null,
    username nvarchar(255) not null,
    password nvarchar(255) not null,
    primary key(serverID)
);

create table phoneDevice (
   deviceID integer not null,
   device nvarchar(255) not null,
   extension nvarchar(255) not null,
   callerID nvarchar(255),
   isPrimary integer not null,
   userID bigint,
   serverID integer not null,
   primary key (deviceID)
);

create table phoneUser (
   userID integer not null,
   username nvarchar(255) unique not null,
   primary key (userID)
);

alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;
alter table phoneDevice add constraint pD_serverID_fk foreign key (serverID) references phoneServer;

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 2);
