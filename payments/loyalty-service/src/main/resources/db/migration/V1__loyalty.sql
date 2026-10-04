CREATE TABLE loyalty_account (
    customer_id     VARCHAR(64) PRIMARY KEY,
    points          BIGINT NOT NULL,
    lifetime_points BIGINT NOT NULL,
    tier            VARCHAR(16) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE loyalty_ledger (
    id             UUID PRIMARY KEY,
    event_id       UUID NOT NULL UNIQUE,
    customer_id    VARCHAR(64) NOT NULL REFERENCES loyalty_account (customer_id),
    transaction_id VARCHAR(64) NOT NULL,
    points         BIGINT NOT NULL,
    entry_type     VARCHAR(16) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_ledger_customer_tx ON loyalty_ledger (customer_id, transaction_id);
