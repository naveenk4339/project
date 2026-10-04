package com.pos.checkout.outbox;

import com.pos.common.events.EventJson;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWriter {

    private final OutboxRepository repository;

    public OutboxWriter(OutboxRepository repository) {
        this.repository = repository;
    }

    /** Must join the caller's transaction: writing an event outside it would break the outbox guarantee. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String aggregateType, String aggregateId, String eventType, Object payload) {
        repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, EventJson.write(payload)));
    }
}
