create table phoneDevice (
   deviceID bigint not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerId varchar(255),
   isPrimary int not null,
   userID bigint,
   primary key (deviceID)
);
create table phoneUser (
   userID bigint not null,
   username varchar(255) not null unique,
   primary key (userID)
);

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 0);