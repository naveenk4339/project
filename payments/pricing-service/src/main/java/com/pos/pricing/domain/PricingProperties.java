package com.pos.pricing.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "pricing")
public record PricingProperties(
        List<Product> products,
        List<Promotion> promotions,
        Map<String, BigDecimal> taxRates,
        BigDecimal defaultTaxRate) {

    public PricingProperties {
        products = products == null ? List.of() : products;
        promotions = promotions == null ? List.of() : promotions;
        taxRates = taxRates == null ? Map.of() : taxRates;
        defaultTaxRate = defaultTaxRate == null ? BigDecimal.ZERO : defaultTaxRate;
    }

    public BigDecimal taxRateFor(String storeId) {
        return taxRates.getOrDefault(storeId, defaultTaxRate);
    }
}
