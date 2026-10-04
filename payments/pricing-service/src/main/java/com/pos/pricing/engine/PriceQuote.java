package com.pos.pricing.engine;

import java.math.BigDecimal;
import java.util.List;

public record PriceQuote(
        String storeId,
        List<Line> lines,
        long subtotalCents,
        long discountCents,
        long taxCents,
        long totalCents,
        BigDecimal taxRate,
        List<String> appliedPromotions) {

    public record Line(
            String sku,
            String name,
            String category,
            int quantity,
            long unitPriceCents,
            long discountCents,
            long lineTotalCents,
            boolean taxable) {
    }
}
