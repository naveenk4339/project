package com.pos.payment.api;

import com.pos.payment.domain.Payment;
import com.pos.payment.domain.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record AuthorizeRequest(
            @NotBlank String transactionId,
            @NotBlank String storeId,
            @NotNull PaymentMethod method,
            @Min(1) long amountCents,
            String cardToken,
            long tenderedCents) {
    }

    public record RefundRequest(@NotBlank String idempotencyKey, @Min(1) long amountCents, String reason) {
    }

    public record PaymentView(UUID id, String transactionId, String method, String status, long amountCents,
                              long tenderedCents, long changeCents, long refundedCents, String cardBrand,
                              String cardLast4, String cardFingerprint, String authCode, String declineReason,
                              double fraudScore, Instant createdAt) {

        public static PaymentView of(Payment p) {
            return new PaymentView(p.getId(), p.getTransactionId(), p.getMethod().name(), p.getStatus().name(),
                    p.getAmountCents(), p.getTenderedCents(), p.getChangeCents(), p.getRefundedCents(),
                    p.getCardBrand(), p.getCardLast4(), p.getCardFingerprint(), p.getAuthCode(),
                    p.getDeclineReason(), p.getFraudScore(), p.getCreatedAt());
        }
    }

    public record RefundView(UUID refundId, UUID paymentId, long amountCents, String paymentStatus) {
    }
}
