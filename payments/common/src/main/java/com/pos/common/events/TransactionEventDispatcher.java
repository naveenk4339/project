package com.pos.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactionEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventDispatcher.class);

    private TransactionEventDispatcher() {
    }

    public static void dispatch(String json, TransactionEventHandler handler) {
        EventEnvelope envelope = EventJson.readEnvelope(json);
        switch (envelope.eventType()) {
            case EventTypes.TRANSACTION_COMPLETED ->
                    handler.onCompleted(envelope, EventJson.payload(envelope, TransactionCompleted.class));
            case EventTypes.TRANSACTION_REFUNDED ->
                    handler.onRefunded(envelope, EventJson.payload(envelope, TransactionRefunded.class));
            default -> log.debug("Ignoring event type {} ({})", envelope.eventType(), envelope.eventId());
        }
    }
}
