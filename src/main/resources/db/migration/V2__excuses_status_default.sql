update excuses set status = 'PENDING' where status is null;

alter table if exists excuses
    alter column status set default 'PENDING';

alter table if exists excuses
    alter column status set not null;
