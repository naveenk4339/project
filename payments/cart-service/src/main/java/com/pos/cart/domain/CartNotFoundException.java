package com.pos.cart.domain;

import java.util.UUID;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(UUID id) {
        super("Cart not found: " + id);
    }
}
