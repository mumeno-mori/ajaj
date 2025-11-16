create table public.indexed_file
(
    id                 uuid default gen_random_uuid() not null
        constraint indexed_file_pk
            primary key,
    project_id         text                           not null,
    app_id             text                           not null,
    path               text                           not null,
    modified_at        timestamp with time zone       null,
    modified_at_stored timestamp with time zone       null,
    is_modified        boolean
        generated always as (
            case
                when modified_at = modified_at_stored then false
                else true
            end) stored
);

create unique index indexed_file_project_id_app_id_path_uindex
    on public.indexed_file (project_id, app_id, path);

create index indexed_file_is_modified_idx on public.indexed_file (is_modified);