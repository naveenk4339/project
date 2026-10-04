package com.pos.common.events;

/** Kafka topic names shared by producers (outbox relay) and consumers. */
public final class Topics {

    /** All transaction lifecycle events, keyed by transaction id so per-transaction ordering holds. */
    public static final String TRANSACTION_EVENTS = "pos.transaction-events";

    /** Dead-letter topic for events a consumer could not process after retries. */
    public static final String TRANSACTION_EVENTS_DLT = TRANSACTION_EVENTS + ".DLT";

    private Topics() {
    }
}
