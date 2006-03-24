create table phoneDevice (
   deviceID integer not null,
   device varchar2(255) not null,
   extension varchar2(255) not null,
   callerID varchar2(255),
   isPrimary integer not null,
   userID integer,
   primary key (deviceID)
);
create table phoneUser (
   userID integer not null,
   username varchar2(255) not null unique,
   primary key (userID)
);
alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;
