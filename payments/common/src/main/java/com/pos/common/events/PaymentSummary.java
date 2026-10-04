package com.pos.common.events;

public record PaymentSummary(
        String paymentId,
        String method,
        String cardBrand,
        String cardLast4,
        String cardFingerprint,
        String authCode,
        long amountCents,
        long tenderedCents,
        long changeCents,
        double fraudScore) {
}
