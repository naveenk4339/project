package com.pos.checkout.api;

import com.pos.checkout.domain.PosTransaction;
import com.pos.common.events.PaymentSummary;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import com.pos.common.events.TransactionRefunded;

import java.time.Instant;
import java.util.List;

/** Maps the persisted transaction to the public event contracts in {@code common}. */
final class TransactionEvents {

    private TransactionEvents() {
    }

    static TransactionCompleted completed(PosTransaction t) {
        return new TransactionCompleted(t.getId(), t.getStoreId(), t.getTerminalId(), t.getCustomerId(), lines(t),
                t.getSubtotalCents(), t.getDiscountCents(), t.getTaxCents(), t.getTotalCents(),
                t.appliedPromotionList(),
                new PaymentSummary(t.getPaymentId(), t.getPaymentMethod(), t.getCardBrand(), t.getCardLast4(),
                        t.getCardFingerprint(), t.getAuthCode(), t.getTotalCents(), t.getTenderedCents(),
                        t.getChangeCents(), t.getFraudScore()),
                t.getCompletedAt());
    }

    static TransactionRefunded refunded(PosTransaction t, String refundId, String reason) {
        return new TransactionRefunded(t.getId(), t.getStoreId(), t.getCustomerId(), t.getPaymentId(), refundId,
                lines(t), t.getRefundedCents(), reason, Instant.now());
    }

    private static List<TransactionLine> lines(PosTransaction t) {
        return t.getLines().stream().map(l -> new TransactionLine(l.getSku(), l.getName(), l.getCategory(),
                l.getQuantity(), l.getUnitPriceCents(), l.getDiscountCents(), l.getLineTotalCents())).toList();
    }
}
