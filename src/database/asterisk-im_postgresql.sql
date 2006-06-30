create table phoneDevice (
   deviceID int8 not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerID varchar(255),
   isPrimary integer not null,
   userID int8,
   primary key (deviceID)
);
create table phoneUser (
   userID int8 not null,
   username varchar(255) not null unique,
   primary key (userID)
);
alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;

INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', 0);
