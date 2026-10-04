CREATE TABLE payment (
    id               UUID PRIMARY KEY,
    transaction_id   VARCHAR(64) NOT NULL UNIQUE,
    store_id         VARCHAR(64) NOT NULL,
    method           VARCHAR(16) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    amount_cents     BIGINT NOT NULL,
    tendered_cents   BIGINT NOT NULL DEFAULT 0,
    change_cents     BIGINT NOT NULL DEFAULT 0,
    refunded_cents   BIGINT NOT NULL DEFAULT 0,
    card_brand       VARCHAR(16),
    card_last4       VARCHAR(4),
    card_fingerprint VARCHAR(64),
    auth_code        VARCHAR(16),
    decline_reason   VARCHAR(64),
    fraud_score      DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE payment_refund (
    id              UUID PRIMARY KEY,
    payment_id      UUID NOT NULL REFERENCES payment (id),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    amount_cents    BIGINT NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
