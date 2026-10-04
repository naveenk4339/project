CREATE TABLE cart (
    id          UUID PRIMARY KEY,
    store_id    VARCHAR(64) NOT NULL,
    terminal_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64),
    status      VARCHAR(16) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE cart_item (
    cart_id  UUID NOT NULL REFERENCES cart (id) ON DELETE CASCADE,
    position INT NOT NULL,
    sku      VARCHAR(64) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (cart_id, position)
);
