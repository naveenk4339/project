-- mirrors checkout-service's migration, which owns this table in the real Transaction DB
CREATE TABLE IF NOT EXISTS outbox_event (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        TEXT NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    attempts       INT NOT NULL DEFAULT 0
);
