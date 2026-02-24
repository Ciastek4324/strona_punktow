alter table if exists weekly_tables
    add column if not exists completed boolean not null default false;
