create table if not exists user_accounts (
  id bigserial primary key,
  password_hash varchar(255) not null,
  role varchar(255) not null,
  username varchar(255) not null
);
create unique index if not exists ux_user_accounts_username on user_accounts(username);

create table if not exists persons (
  id bigserial primary key,
  base_points integer not null default 0,
  display_name varchar(255) not null,
  role varchar(255)
);
create unique index if not exists ux_persons_display_name on persons(display_name);

create table if not exists current_points (
  id bigserial primary key,
  points integer not null,
  person_id bigint not null
);
create unique index if not exists ux_current_points_person on current_points(person_id);

create table if not exists points_snapshots (
  id bigserial primary key,
  snapshot_date date not null
);

create table if not exists weekly_tables (
  id bigserial primary key,
  week_end date,
  week_start date
);

create table if not exists weekly_attendance (
  id bigserial primary key,
  attendance_codes varchar(255),
  person_id bigint not null,
  table_ref_id bigint not null
);

create table if not exists excuses (
  id bigserial primary key,
  created_at timestamp(6) not null,
  created_by varchar(255) not null,
  date_from date,
  date_to date,
  full_name varchar(255) not null,
  read_flag boolean not null,
  reason varchar(1000) not null,
  reviewed_at timestamp(6),
  reviewed_by varchar(255),
  status varchar(255) not null default 'PENDING'
);
create index if not exists ix_excuses_status on excuses(status);

create table if not exists points_history (
  id bigserial primary key,
  changed_at timestamp(6) not null,
  changed_by varchar(255) not null,
  new_points integer not null,
  old_points integer not null,
  person_id bigint not null
);

create table if not exists monthly_points_snapshots (
  id bigserial primary key,
  created_at timestamp(6) not null,
  created_by varchar(255) not null,
  month_date date not null
);

create table if not exists monthly_points_snapshot_items (
  id bigserial primary key,
  base_points integer not null,
  month_points integer not null,
  person_name varchar(255) not null,
  total_points integer not null,
  snapshot_id bigint not null
);