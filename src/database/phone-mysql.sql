alter table phoneDevice drop foreign key FKA6F6FD644F390113;
drop table if exists phoneDevice;
drop table if exists phoneUser;
create table phoneDevice (
   deviceID bigint not null,
   device varchar(255) not null,
   extension varchar(255) not null,
   callerId varchar(255),
   isPrimary bit not null,
   userID bigint,
   primary key (deviceID)
);
create table phoneUser (
   userID bigint not null,
   username varchar(255) not null unique,
   primary key (userID)
);
alter table phoneDevice add index FKA6F6FD644F390113 (userID), add constraint FKA6F6FD644F390113 foreign key (userID) references phoneUser (userID);
