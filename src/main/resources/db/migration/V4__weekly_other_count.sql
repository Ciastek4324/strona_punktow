alter table if exists weekly_attendance
    add column if not exists other_count integer not null default 0;
