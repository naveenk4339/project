package com.pos.pricing.engine;

import com.pos.pricing.domain.PricingProperties;
import com.pos.pricing.domain.Product;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Catalog {

    private final Map<String, Product> bySku;

    public Catalog(PricingProperties properties) {
        this.bySku = properties.products().stream()
                .collect(Collectors.toUnmodifiableMap(Product::sku, Function.identity()));
    }

    public Optional<Product> find(String sku) {
        return Optional.ofNullable(bySku.get(sku));
    }

    public Collection<Product> all() {
        return bySku.values();
    }
}
