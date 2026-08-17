create table if not exists transactions (
    id uuid primary key,
    mti varchar(4) not null default '0100',
    stan varchar(6) not null,
    rrn varchar(12),
    pan varchar(16) not null,
    processing_code varchar(6) not null,
    amount numeric not null,
    currency_code varchar(3) not null,
    terminal_id varchar(8) not null,
    terminal_type varchar(10),
    merchant_id varchar(15) not null,
    mcc varchar(4) not null,
    acquirer_id varchar(10) not null,
    issuer_id varchar(10),
    acquiring_fee numeric,
    status varchar(20) not null,
    decline_reason varchar(100),
    auth_code varchar(6),
    processing_time_ms integer,
    transmission_date_time timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone not null,
    constraint transactions_status_check check (status in ('APPROVED', 'DECLINED'))
);

create index if not exists idx_transactions_status on transactions (status);
create index if not exists idx_transactions_created_at on transactions (created_at);
create index if not exists idx_transactions_pan on transactions (pan);
create index if not exists idx_transactions_merchant on transactions (merchant_id);
