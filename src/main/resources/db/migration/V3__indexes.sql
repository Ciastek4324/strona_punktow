create index if not exists idx_excuses_status_created_at
    on excuses (status, created_at desc);

create index if not exists idx_excuses_created_at
    on excuses (created_at desc);

create index if not exists idx_points_history_changed_at
    on points_history (changed_at desc);

create index if not exists idx_points_history_person
    on points_history (person_id);

create index if not exists idx_monthly_snapshot_created_at
    on monthly_points_snapshot (created_at desc);

create index if not exists idx_monthly_snapshot_item_snapshot
    on monthly_points_snapshot_item (snapshot_id);
