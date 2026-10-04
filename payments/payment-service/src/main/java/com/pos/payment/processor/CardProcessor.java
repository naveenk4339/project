package com.pos.payment.processor;

/**
 * Port to the card network / acquirer. The sample ships a simulator; a real deployment would put an
 * adapter for Stripe Terminal, Adyen, etc. behind the same interface.
 */
public interface CardProcessor {

    CardDetails resolve(String cardToken);

    AuthResult authorizeAndCapture(String cardToken, long amountCents, String reference);

    void refund(String authCode, long amountCents, String reference);

    record CardDetails(String brand, String last4, String fingerprint) {
    }

    record AuthResult(boolean approved, String authCode, String declineReason) {

        public static AuthResult approved(String authCode) {
            return new AuthResult(true, authCode, null);
        }

        public static AuthResult declined(String reason) {
            return new AuthResult(false, null, reason);
        }
    }
}
