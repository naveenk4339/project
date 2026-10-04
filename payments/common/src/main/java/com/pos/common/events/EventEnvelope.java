package com.pos.common.events;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire format of every message on {@link Topics#TRANSACTION_EVENTS}. The {@code eventId} is the
 * outbox row id and is what consumers use to de-duplicate (delivery is at-least-once).
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        JsonNode payload) {
}
