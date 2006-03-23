create table phoneDevice (
   deviceID bigint not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerID varchar(255),
   isPrimary integer not null,
   userID integer,
   constraint phoneDevice_pk primary key (deviceID)
);
create table phoneUser (
   userID bigint not null,
   username varchar(255) not null,
   constraint phoneUser_pk primary key (userID)
);
create unique index phoneUser_username_idx on phoneUser(username);