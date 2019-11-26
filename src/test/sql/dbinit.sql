create user sarah password 'connor';
create table fact (id integer primary key, name varchar unique not null , type varchar);
ALTER TABLE fact OWNER TO sarah;
insert into fact VALUES (1, 'cpu','STRING');
insert into fact VALUES (2, 'cores','INT');
insert into fact VALUES (3, 'rhelversion','STRING');

create table policy (id integer, customerid varchar not null,
                     name varchar not null ,
                     description varchar,
                     is_enabled boolean not null default false,
                     conditions varchar not null,
                     actions varchar );
alter table policy OWNER to sarah;
alter table policy ADD PRIMARY KEY (customerid,id);
create unique index pol_cus_nam_idx on policy (customerid,name); -- TODO does this make the name unique per customer (?)


insert into policy values (1, '1', '1st policy', 'Just a test', true, '"cores" == 1','EMAIL roadrunner@acme.org');
insert into policy values (2, '1', '2nd policy', 'Another test', false, '"cpu" != "intel"','HOOK http://localhost:8080');
insert into policy values (3, '1', '3rd policy', 'Another test', true, '"rhelversion" >= "8" OR "cores" == 5','EMAIL toor@root.org');
insert into policy values (4, '2', '4th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');

-- create a sequence for hibernate id generator. Must start with a number higher than the id values
-- of above tables
create sequence hibernate_sequence start with 100 ;
alter sequence hibernate_sequence owner to sarah;

