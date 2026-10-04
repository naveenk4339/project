package com.pos.payment.api;

import com.pos.payment.api.PaymentDtos.AuthorizeRequest;
import com.pos.payment.api.PaymentDtos.RefundRequest;
import com.pos.payment.api.PaymentDtos.RefundView;
import com.pos.payment.domain.Payment;
import com.pos.payment.domain.PaymentMethod;
import com.pos.payment.domain.PaymentRepository;
import com.pos.payment.domain.Refund;
import com.pos.payment.domain.RefundRepository;
import com.pos.payment.fraud.FraudClient;
import com.pos.payment.fraud.FraudClient.FraudRequest;
import com.pos.payment.fraud.FraudClient.FraudScore;
import com.pos.payment.fraud.FraudProperties;
import com.pos.payment.processor.CardProcessor;
import com.pos.payment.processor.CardProcessor.AuthResult;
import com.pos.payment.processor.CardProcessor.CardDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * Authorization is idempotent per transaction id: the checkout service may retry after a timeout and
 * will get back the original outcome rather than charging the card twice.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final CardProcessor processor;
    private final FraudClient fraud;
    private final FraudProperties fraudProperties;
    private final TransactionTemplate tx;

    public PaymentService(PaymentRepository payments, RefundRepository refunds, CardProcessor processor,
                          FraudClient fraud, FraudProperties fraudProperties, TransactionTemplate tx) {
        this.payments = payments;
        this.refunds = refunds;
        this.processor = processor;
        this.fraud = fraud;
        this.fraudProperties = fraudProperties;
        this.tx = tx;
    }

    public Payment authorize(AuthorizeRequest request) {
        Optional<Payment> existing = payments.findByTransactionId(request.transactionId());
        if (existing.isPresent()) {
            log.info("Replaying payment outcome for transaction {}", request.transactionId());
            return existing.get();
        }
        Payment payment = new Payment(request.transactionId(), request.storeId(), request.method(), request.amountCents());
        if (request.method() == PaymentMethod.CASH) {
            payCash(payment, request);
        } else {
            payCard(payment, request);
        }
        try {
            return tx.execute(status -> payments.saveAndFlush(payment));
        } catch (DataIntegrityViolationException race) {
            // a concurrent retry won the insert; its outcome is the authoritative one
            return payments.findByTransactionId(request.transactionId()).orElseThrow(() -> race);
        }
    }

    private void payCash(Payment payment, AuthorizeRequest request) {
        if (request.tenderedCents() < request.amountCents()) {
            payment.decline("INSUFFICIENT_CASH_TENDERED");
        } else {
            payment.capture(request.tenderedCents(), null);
        }
    }

    private void payCard(Payment payment, AuthorizeRequest request) {
        if (request.cardToken() == null || request.cardToken().isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "CARD_TOKEN_REQUIRED", "cardToken is required for CARD");
        }
        CardDetails card = processor.resolve(request.cardToken());
        payment.card(card.brand(), card.last4(), card.fingerprint());

        Optional<FraudScore> score = fraud.score(new FraudRequest(request.transactionId(), request.storeId(),
                card.fingerprint(), request.method().name(), request.amountCents()));
        if (score.isPresent()) {
            payment.fraudScore(score.get().score());
            if (score.get().score() >= fraudProperties.declineThreshold()) {
                payment.decline("SUSPECTED_FRAUD");
                return;
            }
        } else if (request.amountCents() > fraudProperties.failOpenMaxCents()) {
            payment.decline("FRAUD_CHECK_UNAVAILABLE");
            return;
        }

        AuthResult result = processor.authorizeAndCapture(request.cardToken(), request.amountCents(), request.transactionId());
        if (result.approved()) {
            payment.capture(request.amountCents(), result.authCode());
        } else {
            payment.decline(result.declineReason());
        }
    }

    public Payment get(UUID id) {
        return payments.findById(id).orElseThrow(() ->
                new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found: " + id));
    }

    public Payment byTransaction(String transactionId) {
        return payments.findByTransactionId(transactionId).orElseThrow(() ->
                new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "No payment for " + transactionId));
    }

    public RefundView refund(UUID paymentId, RefundRequest request) {
        return tx.execute(status -> {
            Optional<Refund> replay = refunds.findByIdempotencyKey(request.idempotencyKey());
            if (replay.isPresent()) {
                Payment payment = get(replay.get().getPaymentId());
                return new RefundView(replay.get().getId(), payment.getId(), replay.get().getAmountCents(),
                        payment.getStatus().name());
            }
            Payment payment = get(paymentId);
            if (request.amountCents() > payment.refundable()) {
                throw new PaymentException(HttpStatus.CONFLICT, "REFUND_EXCEEDS_CAPTURED",
                        "Refundable amount is " + payment.refundable() + " cents");
            }
            if (payment.getMethod() == PaymentMethod.CARD) {
                processor.refund(payment.getAuthCode(), request.amountCents(), request.idempotencyKey());
            }
            payment.recordRefund(request.amountCents());
            Refund refund = refunds.save(new Refund(payment.getId(), request.idempotencyKey(),
                    request.amountCents(), request.reason()));
            payments.save(payment);
            return new RefundView(refund.getId(), payment.getId(), refund.getAmountCents(), payment.getStatus().name());
        });
    }
}
