package com.pos.checkout.client;

import java.util.List;

/** Views of the upstream services' responses, limited to the fields checkout relies on. */
public final class ClientDtos {

    private ClientDtos() {
    }

    public record CartSnapshot(String id, String storeId, String terminalId, String customerId, String status,
                               List<Item> items) {

        public record Item(String sku, int quantity) {
        }
    }

    public record QuoteRequest(String storeId, List<CartSnapshot.Item> items) {
    }

    public record Quote(List<Line> lines, long subtotalCents, long discountCents, long taxCents, long totalCents,
                        List<String> appliedPromotions) {

        public record Line(String sku, String name, String category, int quantity, long unitPriceCents,
                           long discountCents, long lineTotalCents) {
        }
    }

    public record AuthorizeRequest(String transactionId, String storeId, String method, long amountCents,
                                   String cardToken, long tenderedCents) {
    }

    public record PaymentResult(String id, String transactionId, String method, String status, long amountCents,
                                long tenderedCents, long changeCents, String cardBrand, String cardLast4,
                                String cardFingerprint, String authCode, String declineReason, double fraudScore) {

        public boolean captured() {
            return "CAPTURED".equals(status);
        }
    }

    public record RefundRequest(String idempotencyKey, long amountCents, String reason) {
    }

    public record RefundResult(String refundId, String paymentId, long amountCents, String paymentStatus) {
    }
}
