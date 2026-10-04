package com.pos.common.events;

/**
 * Implemented by consumers. {@link TransactionEventDispatcher} decodes the envelope and routes it here,
 * which keeps the business logic free of Kafka types and easy to unit test.
 */
public interface TransactionEventHandler {

    default void onCompleted(EventEnvelope envelope, TransactionCompleted event) {
    }

    default void onRefunded(EventEnvelope envelope, TransactionRefunded event) {
    }
}
