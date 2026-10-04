package com.pos.loyalty;

import com.pos.common.events.Topics;
import com.pos.common.events.TransactionEventDispatcher;
import com.pos.common.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService service;

    public LoyaltyController(LoyaltyService service) {
        this.service = service;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<?> account(@PathVariable String customerId) {
        return service.account(customerId).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiError.of("NOT_A_MEMBER", "No loyalty account for " + customerId)));
    }

    @KafkaListener(topics = Topics.TRANSACTION_EVENTS)
    public void on(String message) {
        TransactionEventDispatcher.dispatch(message, service);
    }
}
