alter table if exists weekly_attendance
    add column if not exists day_of_week integer not null default 1,
    add column if not exists present boolean not null default false;
