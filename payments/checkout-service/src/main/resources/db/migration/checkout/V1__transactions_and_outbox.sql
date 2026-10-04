CREATE TABLE pos_transaction (
    id                 VARCHAR(64) PRIMARY KEY,
    idempotency_key    VARCHAR(128) NOT NULL UNIQUE,
    cart_id            VARCHAR(64) NOT NULL,
    store_id           VARCHAR(64) NOT NULL,
    terminal_id        VARCHAR(64) NOT NULL,
    customer_id        VARCHAR(64),
    status             VARCHAR(16) NOT NULL,
    subtotal_cents     BIGINT NOT NULL,
    discount_cents     BIGINT NOT NULL,
    tax_cents          BIGINT NOT NULL,
    total_cents        BIGINT NOT NULL,
    applied_promotions VARCHAR(512),
    payment_method     VARCHAR(16) NOT NULL,
    payment_id         VARCHAR(64),
    card_brand         VARCHAR(16),
    card_last4         VARCHAR(4),
    card_fingerprint   VARCHAR(64),
    auth_code          VARCHAR(16),
    tendered_cents     BIGINT NOT NULL DEFAULT 0,
    change_cents       BIGINT NOT NULL DEFAULT 0,
    fraud_score        DOUBLE PRECISION NOT NULL DEFAULT 0,
    decline_reason     VARCHAR(64),
    refunded_cents     BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at       TIMESTAMP WITH TIME ZONE,
    version            BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_pos_transaction_store_created ON pos_transaction (store_id, created_at DESC);

CREATE TABLE pos_transaction_line (
    transaction_id   VARCHAR(64) NOT NULL REFERENCES pos_transaction (id) ON DELETE CASCADE,
    position         INT NOT NULL,
    sku              VARCHAR(64) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    category         VARCHAR(64) NOT NULL,
    quantity         INT NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    discount_cents   BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, position)
);

-- Transactional outbox: written in the same transaction as pos_transaction, drained by outbox-relay.
CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        TEXT NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    attempts       INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_unpublished ON outbox_event (published_at, created_at);
