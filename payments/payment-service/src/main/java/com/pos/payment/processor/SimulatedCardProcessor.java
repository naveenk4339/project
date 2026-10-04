package com.pos.payment.processor;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Deterministic test processor. Tokens mimic what a P2PE card reader would hand the POS:
 * <ul>
 *   <li>{@code tok_visa}, {@code tok_mastercard}, {@code tok_amex}: approved</li>
 *   <li>{@code tok_decline}: declined (insufficient funds)</li>
 *   <li>{@code tok_expired}: declined (expired card)</li>
 *   <li>{@code tok_limit_<cents>}: approved up to that amount, declined above it</li>
 * </ul>
 */
@Component
public class SimulatedCardProcessor implements CardProcessor {

    private final SecureRandom random = new SecureRandom();

    @Override
    public CardDetails resolve(String cardToken) {
        String token = cardToken.toLowerCase(Locale.ROOT);
        String brand = token.contains("mastercard") ? "MASTERCARD" : token.contains("amex") ? "AMEX" : "VISA";
        String fingerprint = sha256(cardToken);
        String last4 = String.valueOf(1000 + Math.floorMod(fingerprint.hashCode(), 9000));
        return new CardDetails(brand, last4, fingerprint);
    }

    @Override
    public AuthResult authorizeAndCapture(String cardToken, long amountCents, String reference) {
        String token = cardToken.toLowerCase(Locale.ROOT);
        if (token.equals("tok_decline")) {
            return AuthResult.declined("INSUFFICIENT_FUNDS");
        }
        if (token.equals("tok_expired")) {
            return AuthResult.declined("EXPIRED_CARD");
        }
        if (token.startsWith("tok_limit_")) {
            long limit = Long.parseLong(token.substring("tok_limit_".length()));
            if (amountCents > limit) {
                return AuthResult.declined("INSUFFICIENT_FUNDS");
            }
        }
        return AuthResult.approved(String.format("%06d", random.nextInt(1_000_000)));
    }

    @Override
    public void refund(String authCode, long amountCents, String reference) {
        // simulator: refunds always succeed
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
