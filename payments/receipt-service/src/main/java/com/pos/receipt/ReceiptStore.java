package com.pos.receipt;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionEventHandler;
import com.pos.common.events.TransactionRefunded;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory receipt store keyed by transaction id, so redeliveries simply overwrite with the same receipt.
 * A production deployment would write to object storage and e-mail/SMS the link; the consumer logic is the same.
 */
@Component
public class ReceiptStore implements TransactionEventHandler {

    private final Map<String, Receipt> receipts = new ConcurrentHashMap<>();
    private final ReceiptRenderer renderer;

    public ReceiptStore(ReceiptRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void onCompleted(EventEnvelope envelope, TransactionCompleted sale) {
        receipts.putIfAbsent(sale.transactionId(), new Receipt(sale.transactionId(), sale.storeId(), sale.customerId(),
                sale, renderer.render(sale, false), false, null));
    }

    @Override
    public void onRefunded(EventEnvelope envelope, TransactionRefunded refund) {
        receipts.computeIfPresent(refund.transactionId(),
                (id, r) -> r.markRefunded(renderer.render(r.sale(), true), refund.refundedAt()));
    }

    public Optional<Receipt> find(String transactionId) {
        return Optional.ofNullable(receipts.get(transactionId));
    }
}
