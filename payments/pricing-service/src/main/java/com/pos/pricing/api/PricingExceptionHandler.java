package com.pos.pricing.api;

import com.pos.common.web.ApiError;
import com.pos.pricing.engine.UnknownSkuException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PricingExceptionHandler {

    @ExceptionHandler(UnknownSkuException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError unknownSku(UnknownSkuException e) {
        return ApiError.of("UNKNOWN_SKU", e.getMessage());
    }
}
