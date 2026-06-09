alter table transactions
  add column if not exists bank_category text;

alter table transactions
  add column if not exists card_ref text;

alter table transactions
  add column if not exists trans_date date;
