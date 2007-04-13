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

alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 2);
