package com.pos.receipt;

import com.pos.common.events.TransactionCompleted;

import java.time.Instant;

public record Receipt(String transactionId, String storeId, String customerId, TransactionCompleted sale,
                      String text, boolean refunded, Instant refundedAt) {

    Receipt markRefunded(String text, Instant at) {
        return new Receipt(transactionId, storeId, customerId, sale, text, true, at);
    }
}
