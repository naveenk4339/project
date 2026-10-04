package com.pos.common.events;

public record TransactionLine(
        String sku,
        String name,
        String category,
        int quantity,
        long unitPriceCents,
        long discountCents,
        long lineTotalCents) {
}
