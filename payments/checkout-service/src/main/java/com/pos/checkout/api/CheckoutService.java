package com.pos.checkout.api;

import com.pos.checkout.api.CheckoutDtos.CheckoutRequest;
import com.pos.checkout.client.CartClient;
import com.pos.checkout.client.ClientDtos.AuthorizeRequest;
import com.pos.checkout.client.ClientDtos.CartSnapshot;
import com.pos.checkout.client.ClientDtos.PaymentResult;
import com.pos.checkout.client.ClientDtos.Quote;
import com.pos.checkout.client.ClientDtos.QuoteRequest;
import com.pos.checkout.client.ClientDtos.RefundRequest;
import com.pos.checkout.client.ClientDtos.RefundResult;
import com.pos.checkout.client.PaymentClient;
import com.pos.checkout.client.PricingClient;
import com.pos.checkout.client.UpstreamException;
import com.pos.checkout.domain.PosTransaction;
import com.pos.checkout.domain.PosTransactionRepository;
import com.pos.checkout.domain.TransactionStatus;
import com.pos.checkout.outbox.OutboxWriter;
import com.pos.common.events.EventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Checkout saga. Remote calls (cart, pricing, payment) happen outside database transactions; each local
 * state change is its own short transaction, and the one that completes the sale also writes the
 * TransactionCompleted outbox row. Every step is safe to repeat, so the POS simply retries with the same
 * {@code Idempotency-Key} after a timeout.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final String AGGREGATE = "PosTransaction";

    private final PosTransactionRepository transactions;
    private final OutboxWriter outbox;
    private final CartClient carts;
    private final PricingClient pricing;
    private final PaymentClient payments;
    private final TransactionTemplate tx;

    public CheckoutService(PosTransactionRepository transactions, OutboxWriter outbox, CartClient carts,
                           PricingClient pricing, PaymentClient payments, TransactionTemplate tx) {
        this.transactions = transactions;
        this.outbox = outbox;
        this.carts = carts;
        this.pricing = pricing;
        this.payments = payments;
        this.tx = tx;
    }

    public PosTransaction checkout(String idempotencyKey, CheckoutRequest request) {
        PosTransaction transaction = transactions.findByIdempotencyKey(idempotencyKey)
                .map(existing -> requireSameCart(existing, request))
                .orElseGet(() -> start(idempotencyKey, request));
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return transaction; // replay of a finished checkout
        }
        return pay(transaction, request);
    }

    private PosTransaction start(String idempotencyKey, CheckoutRequest request) {
        CartSnapshot cart = carts.lock(request.cartId());
        try {
            Quote quote = pricing.quote(new QuoteRequest(cart.storeId(), cart.items()));
            PosTransaction created = new PosTransaction(newTransactionId(), idempotencyKey, cart.id(), cart.storeId(),
                    cart.terminalId(), cart.customerId(), request.payment().method(), quote);
            return tx.execute(status -> transactions.saveAndFlush(created));
        } catch (DataIntegrityViolationException concurrentRetry) {
            return transactions.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> concurrentRetry);
        } catch (RuntimeException e) {
            releaseCart(cart.id());
            throw e;
        }
    }

    private PosTransaction pay(PosTransaction transaction, CheckoutRequest request) {
        PaymentResult payment;
        try {
            payment = payments.authorize(new AuthorizeRequest(transaction.getId(), transaction.getStoreId(),
                    transaction.getPaymentMethod(), transaction.getTotalCents(), request.payment().cardToken(),
                    request.payment().tenderedCents()));
        } catch (UpstreamException e) {
            throw e;
        } catch (RestClientException e) {
            // outcome unknown: keep PENDING; payment-service is idempotent on transaction id, so a retry is safe
            log.warn("Payment outcome unknown for {}: {}", transaction.getId(), e.getMessage());
            throw new CheckoutException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PENDING",
                    "Payment status unknown for " + transaction.getId() + "; retry with the same Idempotency-Key");
        }

        PosTransaction result = tx.execute(status -> {
            PosTransaction fresh = transactions.findById(transaction.getId()).orElseThrow();
            if (fresh.getStatus() != TransactionStatus.PENDING) {
                return fresh; // a concurrent retry already recorded the outcome
            }
            fresh.applyPayment(payment);
            if (fresh.getStatus() == TransactionStatus.COMPLETED) {
                outbox.append(AGGREGATE, fresh.getId(), EventTypes.TRANSACTION_COMPLETED, TransactionEvents.completed(fresh));
            }
            return transactions.save(fresh);
        });

        if (result.getStatus() == TransactionStatus.COMPLETED) {
            closeCart(result.getCartId());
        } else if (result.getStatus() == TransactionStatus.DECLINED) {
            releaseCart(result.getCartId());
        }
        return result;
    }

    public PosTransaction refund(String transactionId, String idempotencyKey, String reason) {
        PosTransaction transaction = get(transactionId);
        if (transaction.getStatus() == TransactionStatus.REFUNDED) {
            return transaction;
        }
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new CheckoutException(HttpStatus.CONFLICT, "NOT_REFUNDABLE",
                    "Transaction " + transactionId + " is " + transaction.getStatus());
        }
        RefundResult refund = payments.refund(transaction.getPaymentId(),
                new RefundRequest(idempotencyKey, transaction.getTotalCents(), reason));
        return tx.execute(status -> {
            PosTransaction fresh = transactions.findById(transactionId).orElseThrow();
            if (fresh.getStatus() == TransactionStatus.REFUNDED) {
                return fresh;
            }
            fresh.markRefunded(refund.amountCents());
            outbox.append(AGGREGATE, fresh.getId(), EventTypes.TRANSACTION_REFUNDED,
                    TransactionEvents.refunded(fresh, refund.refundId(), reason));
            return transactions.save(fresh);
        });
    }

    public PosTransaction get(String transactionId) {
        return transactions.findById(transactionId).orElseThrow(() ->
                new CheckoutException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Unknown transaction " + transactionId));
    }

    public List<PosTransaction> recent(String storeId, int limit) {
        return transactions.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(0, Math.min(limit, 100)));
    }

    private static PosTransaction requireSameCart(PosTransaction existing, CheckoutRequest request) {
        if (!existing.getCartId().equals(request.cartId())) {
            throw new CheckoutException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was already used for cart " + existing.getCartId());
        }
        return existing;
    }

    private void closeCart(String cartId) {
        try {
            carts.complete(cartId);
        } catch (RuntimeException e) {
            // the sale is final regardless; the cart stays LOCKED and can be closed by a later retry
            log.warn("Could not close cart {}: {}", cartId, e.getMessage());
        }
    }

    private void releaseCart(String cartId) {
        try {
            carts.unlock(cartId);
        } catch (RuntimeException e) {
            log.warn("Could not unlock cart {}: {}", cartId, e.getMessage());
        }
    }

    private static String newTransactionId() {
        String day = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");
        return "TX-" + day + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
