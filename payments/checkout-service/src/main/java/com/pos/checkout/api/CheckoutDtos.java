package com.pos.checkout.api;

import com.pos.checkout.domain.PosTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

public final class CheckoutDtos {

    private CheckoutDtos() {
    }

    public record CheckoutRequest(@NotBlank String cartId, @NotNull @Valid Tender payment) {
    }

    public record Tender(@NotNull @Pattern(regexp = "CARD|CASH") String method, String cardToken, long tenderedCents) {
    }

    public record RefundRequest(String reason) {
    }

    public record TransactionView(String id, String status, String cartId, String storeId, String terminalId,
                                  String customerId, List<Line> lines, long subtotalCents, long discountCents,
                                  long taxCents, long totalCents, List<String> appliedPromotions, Payment payment,
                                  String declineReason, long refundedCents, Instant createdAt, Instant completedAt) {

        public record Line(String sku, String name, int quantity, long unitPriceCents, long discountCents,
                           long lineTotalCents) {
        }

        public record Payment(String paymentId, String method, String cardBrand, String cardLast4, String authCode,
                              long tenderedCents, long changeCents, double fraudScore) {
        }

        public static TransactionView of(PosTransaction t) {
            return new TransactionView(t.getId(), t.getStatus().name(), t.getCartId(), t.getStoreId(),
                    t.getTerminalId(), t.getCustomerId(),
                    t.getLines().stream().map(l -> new Line(l.getSku(), l.getName(), l.getQuantity(),
                            l.getUnitPriceCents(), l.getDiscountCents(), l.getLineTotalCents())).toList(),
                    t.getSubtotalCents(), t.getDiscountCents(), t.getTaxCents(), t.getTotalCents(),
                    t.appliedPromotionList(),
                    new Payment(t.getPaymentId(), t.getPaymentMethod(), t.getCardBrand(), t.getCardLast4(),
                            t.getAuthCode(), t.getTenderedCents(), t.getChangeCents(), t.getFraudScore()),
                    t.getDeclineReason(), t.getRefundedCents(), t.getCreatedAt(), t.getCompletedAt());
        }
    }
}
