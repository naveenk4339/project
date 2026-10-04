package com.pos.pricing.engine;

public class UnknownSkuException extends RuntimeException {

    public UnknownSkuException(String sku) {
        super("Unknown SKU: " + sku);
    }
}
