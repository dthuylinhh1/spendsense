create table if not exists accounts (
  id bigserial primary key,
  name text not null
);

create table if not exists transactions (
  id bigserial primary key,
  account_id bigint not null references accounts(id),
  posted_date date not null,
  description text not null,
  amount_cents bigint not null,
  currency char(3) not null default 'CAD',
  source text not null default 'CIBC',
  import_hash text not null,
  created_at timestamptz not null default now()
);

create unique index if not exists ux_transactions_import_hash
  on transactions(import_hash);

insert into accounts(name)
values ('CIBC Chequing')
on conflict do nothing;
