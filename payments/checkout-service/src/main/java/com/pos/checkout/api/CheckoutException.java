package com.pos.checkout.api;

import org.springframework.http.HttpStatus;

public class CheckoutException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public CheckoutException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
