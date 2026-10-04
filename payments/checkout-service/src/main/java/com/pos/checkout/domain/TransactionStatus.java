package com.pos.checkout.domain;

/**
 * PENDING: priced and persisted, payment outcome not yet known (a retry with the same idempotency key resumes it).
 * COMPLETED: paid; TransactionCompleted is in the outbox. DECLINED: payment refused. REFUNDED: fully refunded.
 */
public enum TransactionStatus { PENDING, COMPLETED, DECLINED, REFUNDED }
