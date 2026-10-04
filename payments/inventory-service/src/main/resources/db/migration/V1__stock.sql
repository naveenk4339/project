CREATE TABLE stock (
    sku           VARCHAR(64) PRIMARY KEY,
    on_hand       INT NOT NULL,
    reorder_point INT NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE processed_event (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO stock (sku, on_hand, reorder_point, updated_at) VALUES
    ('COF-001', 500, 50, CURRENT_TIMESTAMP),
    ('COF-002', 300, 40, CURRENT_TIMESTAMP),
    ('BAK-001', 60, 15, CURRENT_TIMESTAMP),
    ('BAK-002', 60, 15, CURRENT_TIMESTAMP),
    ('SNK-001', 120, 20, CURRENT_TIMESTAMP),
    ('SNK-002', 120, 20, CURRENT_TIMESTAMP),
    ('ELE-001', 40, 10, CURRENT_TIMESTAMP),
    ('ELE-002', 12, 5, CURRENT_TIMESTAMP),
    ('MER-001', 25, 5, CURRENT_TIMESTAMP),
    ('MER-002', 30, 5, CURRENT_TIMESTAMP);
