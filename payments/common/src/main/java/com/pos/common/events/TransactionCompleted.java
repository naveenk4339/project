package com.pos.common.events;

import java.time.Instant;
import java.util.List;

public record TransactionCompleted(
        String transactionId,
        String storeId,
        String terminalId,
        String customerId,
        List<TransactionLine> lines,
        long subtotalCents,
        long discountCents,
        long taxCents,
        long totalCents,
        List<String> appliedPromotions,
        PaymentSummary payment,
        Instant completedAt) {
}
