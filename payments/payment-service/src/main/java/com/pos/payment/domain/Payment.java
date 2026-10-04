package com.pos.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    private UUID id;

    /** One payment per POS transaction: the unique constraint is what makes authorize idempotent. */
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "tendered_cents", nullable = false)
    private long tenderedCents;

    @Column(name = "change_cents", nullable = false)
    private long changeCents;

    @Column(name = "refunded_cents", nullable = false)
    private long refundedCents;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_fingerprint")
    private String cardFingerprint;

    @Column(name = "auth_code")
    private String authCode;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "fraud_score", nullable = false)
    private double fraudScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Payment() {
    }

    public Payment(String transactionId, String storeId, PaymentMethod method, long amountCents) {
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.storeId = storeId;
        this.method = method;
        this.amountCents = amountCents;
        this.createdAt = Instant.now();
    }

    public void capture(long tenderedCents, String authCode) {
        this.status = PaymentStatus.CAPTURED;
        this.tenderedCents = tenderedCents;
        this.changeCents = tenderedCents - amountCents;
        this.authCode = authCode;
    }

    public void decline(String reason) {
        this.status = PaymentStatus.DECLINED;
        this.declineReason = reason;
    }

    public void card(String brand, String last4, String fingerprint) {
        this.cardBrand = brand;
        this.cardLast4 = last4;
        this.cardFingerprint = fingerprint;
    }

    public void fraudScore(double score) {
        this.fraudScore = score;
    }

    public long refundable() {
        return status == PaymentStatus.CAPTURED || status == PaymentStatus.PARTIALLY_REFUNDED
                ? amountCents - refundedCents : 0;
    }

    public void recordRefund(long cents) {
        refundedCents += cents;
        status = refundedCents >= amountCents ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public UUID getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getStoreId() { return storeId; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public long getAmountCents() { return amountCents; }
    public long getTenderedCents() { return tenderedCents; }
    public long getChangeCents() { return changeCents; }
    public long getRefundedCents() { return refundedCents; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLast4() { return cardLast4; }
    public String getCardFingerprint() { return cardFingerprint; }
    public String getAuthCode() { return authCode; }
    public String getDeclineReason() { return declineReason; }
    public double getFraudScore() { return fraudScore; }
    public Instant getCreatedAt() { return createdAt; }
}
