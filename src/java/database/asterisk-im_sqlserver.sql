create table phoneDevice (
   deviceID bigint not null,
   device nvarchar(255) not null,
   extension nvarchar(255) not null,
   callerID nvarchar(255),
   isPrimary integer not null,
   userID bigint,
   primary key (deviceID)
);
create table phoneUser (
   userID bigint not null,
   username nvarchar(255) unique not null,
   primary key (userID)
);
alter table phoneDevice add constraint pD_userID_fk foreign key (userID) references phoneUser;
