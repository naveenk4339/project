package com.pos.receipt;

import com.pos.common.events.Topics;
import com.pos.common.events.TransactionEventDispatcher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptStore store;

    public ReceiptController(ReceiptStore store) {
        this.store = store;
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Receipt> receipt(@PathVariable String transactionId) {
        return ResponseEntity.of(store.find(transactionId));
    }

    @GetMapping(value = "/{transactionId}/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> text(@PathVariable String transactionId) {
        return ResponseEntity.of(store.find(transactionId).map(Receipt::text));
    }

    @KafkaListener(topics = Topics.TRANSACTION_EVENTS)
    public void on(String message) {
        TransactionEventDispatcher.dispatch(message, store);
    }
}
