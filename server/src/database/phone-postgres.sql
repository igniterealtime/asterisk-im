alter table phoneDevice drop constraint FKA6F6FD644F390113;
drop table phoneDevice;
drop table phoneUser;
create table phoneDevice (
   deviceID int8 not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerId varchar(255),
   isPrimary bool not null,
   userID int8,
   primary key (deviceID)
);
create table phoneUser (
   userID int8 not null,
   username varchar(255) not null unique,
   primary key (userID)
);
alter table phoneDevice add constraint FKA6F6FD644F390113 foreign key (userID) references phoneUser;
