create table phoneServer (
    serverID bigint not null,
    serverName varchar(255) not null unique,
    hostname varchar(255) not null,
    port int not null,
    username varchar(255) not null,
    password varchar(255) not null,
    primary key(serverID)
);

create table phoneDevice (
    deviceID bigint not null,
    device varchar(255) not null,
    extension varchar(255) not null,
    callerId varchar(255),
    isPrimary int not null,
    userID bigint,
    serverID bigint not null,
    primary key (deviceID)
);

create table phoneUser (
    userID bigint not null,
    username varchar(255) not null unique,
    primary key (userID)
);

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 2);