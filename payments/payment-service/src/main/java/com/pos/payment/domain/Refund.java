package com.pos.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refund")
public class Refund {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    /** Caller-supplied key; retrying a refund with the same key returns the original refund. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Refund() {
    }

    public Refund(UUID paymentId, String idempotencyKey, long amountCents, String reason) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.idempotencyKey = idempotencyKey;
        this.amountCents = amountCents;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getAmountCents() { return amountCents; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
