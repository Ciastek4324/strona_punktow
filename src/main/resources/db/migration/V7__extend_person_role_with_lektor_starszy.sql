do $$
declare
    rec record;
begin
    -- Drop old role check constraints (generated previously for enum values)
    for rec in
        select c.conname, c.conrelid::regclass as table_name
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        join pg_namespace n on n.oid = t.relnamespace
        where c.contype = 'c'
          and n.nspname = 'public'
          and t.relname in ('people', 'persons')
          and pg_get_constraintdef(c.oid) ilike '%role%'
    loop
        execute format('alter table %s drop constraint %I', rec.table_name, rec.conname);
    end loop;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public' and table_name = 'people'
    ) then
        execute 'alter table public.people
                 add constraint chk_people_role_values
                 check (role in (''MINISTRANT'', ''LEKTOR'', ''LEKTOR_STARSZY'', ''ASPIRANT''))';
    end if;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public' and table_name = 'persons'
    ) then
        execute 'alter table public.persons
                 add constraint chk_persons_role_values
                 check (role in (''MINISTRANT'', ''LEKTOR'', ''LEKTOR_STARSZY'', ''ASPIRANT''))';
    end if;
end
$$;

