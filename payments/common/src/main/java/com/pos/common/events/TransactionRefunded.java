package com.pos.common.events;

import java.time.Instant;
import java.util.List;

public record TransactionRefunded(
        String transactionId,
        String storeId,
        String customerId,
        String paymentId,
        String refundId,
        List<TransactionLine> lines,
        long refundedCents,
        String reason,
        Instant refundedAt) {
}
