create table public.indexed_file_document
(
    id              uuid not null
        constraint indexed_file_document_pk
            primary key,
    indexed_file_id uuid not null
        constraint indexed_file_document_indexed_file_id_fk
            references public.indexed_file,
    document_id text not null
);

create index indexed_file_document_indexed_file_id_index
    on public.indexed_file_document (indexed_file_id);

