create sequence if not exists acquirer_fee_seq INCREMENT by 100 start with 1;

create table if not exists acquirer_fee (
    id  BIGINT not null default nextval('acquirer_fee_seq') PRIMARY KEY,
    transmission_date_time TIMESTAMPTZ NOT NULL,
    stan    VARCHAR(6) NOT NULL,
    pan VARCHAR(19) NOT NULL,
    terminal_id VARCHAR(20) NOT NULL,
    acquirer_fee NUMERIC(19, 0) NOT NULL,
    amount NUMERIC(19, 0) NOT NULL,
    CONSTRAINT uk_acquirer_fee_tx UNIQUE (terminal_id, stan, transmission_date_time)
);

alter sequence acquirer_fee_seq owned by acquirer_fee.id;
