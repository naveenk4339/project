package com.pos.cart.api;

import com.pos.cart.domain.CartNotFoundException;
import com.pos.cart.domain.CartStateException;
import com.pos.common.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CartExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(CartNotFoundException e) {
        return ApiError.of("CART_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(CartStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError state(CartStateException e) {
        return ApiError.of("CART_STATE", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError concurrentUpdate() {
        return ApiError.of("CONCURRENT_UPDATE", "Cart was modified concurrently, retry");
    }
}
