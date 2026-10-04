package com.pos.cart.domain;

/**
 * OPEN carts are editable. Checkout moves a cart to LOCKED while payment is in flight so it cannot be
 * changed or paid twice, then to CHECKED_OUT on success or back to OPEN if the payment is declined.
 */
public enum CartStatus { OPEN, LOCKED, CHECKED_OUT }
