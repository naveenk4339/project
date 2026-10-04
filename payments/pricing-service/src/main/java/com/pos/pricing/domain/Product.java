package com.pos.pricing.domain;

public record Product(String sku, String name, String category, long priceCents, boolean taxable) {
}
