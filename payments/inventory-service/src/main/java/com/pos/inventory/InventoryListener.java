package com.pos.inventory;

import com.pos.common.events.Topics;
import com.pos.common.events.TransactionEventDispatcher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryListener {

    private final InventoryEventHandler handler;

    public InventoryListener(InventoryEventHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.TRANSACTION_EVENTS)
    public void on(String message) {
        TransactionEventDispatcher.dispatch(message, handler);
    }
}
