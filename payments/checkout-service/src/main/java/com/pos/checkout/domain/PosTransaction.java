package com.pos.checkout.domain;

import com.pos.checkout.client.ClientDtos.PaymentResult;
import com.pos.checkout.client.ClientDtos.Quote;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "pos_transaction")
public class PosTransaction {

    @Id
    private String id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "cart_id", nullable = false)
    private String cartId;
    @Column(name = "store_id", nullable = false)
    private String storeId;
    @Column(name = "terminal_id", nullable = false)
    private String terminalId;
    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pos_transaction_line", joinColumns = @JoinColumn(name = "transaction_id"))
    @OrderColumn(name = "position")
    private List<TransactionLineItem> lines = new ArrayList<>();

    @Column(name = "subtotal_cents", nullable = false)
    private long subtotalCents;
    @Column(name = "discount_cents", nullable = false)
    private long discountCents;
    @Column(name = "tax_cents", nullable = false)
    private long taxCents;
    @Column(name = "total_cents", nullable = false)
    private long totalCents;
    @Column(name = "applied_promotions")
    private String appliedPromotions;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;
    @Column(name = "payment_id")
    private String paymentId;
    @Column(name = "card_brand")
    private String cardBrand;
    @Column(name = "card_last4")
    private String cardLast4;
    @Column(name = "card_fingerprint")
    private String cardFingerprint;
    @Column(name = "auth_code")
    private String authCode;
    @Column(name = "tendered_cents", nullable = false)
    private long tenderedCents;
    @Column(name = "change_cents", nullable = false)
    private long changeCents;
    @Column(name = "fraud_score", nullable = false)
    private double fraudScore;
    @Column(name = "decline_reason")
    private String declineReason;
    @Column(name = "refunded_cents", nullable = false)
    private long refundedCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    protected PosTransaction() {
    }

    public PosTransaction(String id, String idempotencyKey, String cartId, String storeId, String terminalId,
                          String customerId, String paymentMethod, Quote quote) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.cartId = cartId;
        this.storeId = storeId;
        this.terminalId = terminalId;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.status = TransactionStatus.PENDING;
        this.subtotalCents = quote.subtotalCents();
        this.discountCents = quote.discountCents();
        this.taxCents = quote.taxCents();
        this.totalCents = quote.totalCents();
        this.appliedPromotions = String.join(",", quote.appliedPromotions());
        quote.lines().forEach(l -> lines.add(new TransactionLineItem(l.sku(), l.name(), l.category(), l.quantity(),
                l.unitPriceCents(), l.discountCents(), l.lineTotalCents())));
        this.createdAt = Instant.now();
    }

    public void applyPayment(PaymentResult payment) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction " + id + " is already " + status);
        }
        this.paymentId = payment.id();
        this.cardBrand = payment.cardBrand();
        this.cardLast4 = payment.cardLast4();
        this.cardFingerprint = payment.cardFingerprint();
        this.authCode = payment.authCode();
        this.tenderedCents = payment.tenderedCents();
        this.changeCents = payment.changeCents();
        this.fraudScore = payment.fraudScore();
        if (payment.captured()) {
            this.status = TransactionStatus.COMPLETED;
            this.completedAt = Instant.now();
        } else {
            this.status = TransactionStatus.DECLINED;
            this.declineReason = payment.declineReason();
        }
    }

    public void markRefunded(long cents) {
        if (status != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Only completed transactions can be refunded; " + id + " is " + status);
        }
        this.refundedCents = cents;
        this.status = TransactionStatus.REFUNDED;
    }

    public List<String> appliedPromotionList() {
        return appliedPromotions == null || appliedPromotions.isBlank()
                ? List.of() : Arrays.asList(appliedPromotions.split(","));
    }

    public String getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCartId() { return cartId; }
    public String getStoreId() { return storeId; }
    public String getTerminalId() { return terminalId; }
    public String getCustomerId() { return customerId; }
    public TransactionStatus getStatus() { return status; }
    public List<TransactionLineItem> getLines() { return List.copyOf(lines); }
    public long getSubtotalCents() { return subtotalCents; }
    public long getDiscountCents() { return discountCents; }
    public long getTaxCents() { return taxCents; }
    public long getTotalCents() { return totalCents; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentId() { return paymentId; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLast4() { return cardLast4; }
    public String getCardFingerprint() { return cardFingerprint; }
    public String getAuthCode() { return authCode; }
    public long getTenderedCents() { return tenderedCents; }
    public long getChangeCents() { return changeCents; }
    public double getFraudScore() { return fraudScore; }
    public String getDeclineReason() { return declineReason; }
    public long getRefundedCents() { return refundedCents; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
