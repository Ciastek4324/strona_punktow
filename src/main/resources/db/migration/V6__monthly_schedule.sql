create table if not exists monthly_schedule (
    id bigserial primary key,
    month_date date not null,
    created_at timestamp not null default now()
);

create unique index if not exists idx_monthly_schedule_month
    on monthly_schedule (month_date);

create table if not exists monthly_schedule_entry (
    id bigserial primary key,
    schedule_id bigint not null references monthly_schedule(id) on delete cascade,
    person_id bigint not null references people(id) on delete cascade,
    slot_code integer not null,
    position integer not null
);

create index if not exists idx_monthly_schedule_entry_schedule
    on monthly_schedule_entry (schedule_id);
