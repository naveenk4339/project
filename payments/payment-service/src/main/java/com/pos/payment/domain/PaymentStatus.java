package com.pos.payment.domain;

/** POS sales are authorized and captured in one step, so a successful payment is CAPTURED directly. */
public enum PaymentStatus { CAPTURED, DECLINED, REFUNDED, PARTIALLY_REFUNDED }
