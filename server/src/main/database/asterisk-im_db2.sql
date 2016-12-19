create table phoneServer (
    serverID bigint not null,
    serverName varchar(255) not null unique,
    hostname varchar(255) not null,
    port integer not null,
    username varchar(255) not null,
    password varchar(255) not null,
    constraint phoneServer_pk key(serverID)
);

create table phoneDevice (
   deviceID bigint not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerID varchar(255),
   isPrimary integer not null,
   userID bigint,
   serverID bigint not null,
   primary key (deviceID)
);

create table phoneUser (
   userID bigint not null,
   username varchar(255) unique not null,
   primary key (userID)
);

alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;
alter table phoneDevice add constraint pD_serverID_fk foreign key (serverID) references phoneServer;

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 2);
