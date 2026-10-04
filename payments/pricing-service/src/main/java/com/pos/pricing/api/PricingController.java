package com.pos.pricing.api;

import com.pos.pricing.domain.Product;
import com.pos.pricing.domain.Promotion;
import com.pos.pricing.domain.PricingProperties;
import com.pos.pricing.engine.Catalog;
import com.pos.pricing.engine.PriceQuote;
import com.pos.pricing.engine.PricingEngine;
import com.pos.pricing.engine.QuoteRequest;
import com.pos.pricing.engine.UnknownSkuException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class PricingController {

    private final Catalog catalog;
    private final PricingEngine engine;
    private final PricingProperties properties;

    public PricingController(Catalog catalog, PricingEngine engine, PricingProperties properties) {
        this.catalog = catalog;
        this.engine = engine;
        this.properties = properties;
    }

    @GetMapping("/products")
    public List<Product> products(@RequestParam(required = false) String q) {
        String needle = q == null ? "" : q.toLowerCase(Locale.ROOT);
        return catalog.all().stream()
                .filter(p -> needle.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(needle)
                        || p.sku().toLowerCase(Locale.ROOT).contains(needle)
                        || p.category().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    @GetMapping("/products/{sku}")
    public Product product(@PathVariable String sku) {
        return catalog.find(sku).orElseThrow(() -> new UnknownSkuException(sku));
    }

    @GetMapping("/promotions")
    public List<Promotion> promotions() {
        return properties.promotions();
    }

    @PostMapping("/pricing/quote")
    public PriceQuote quote(@Valid @RequestBody QuoteRequest request) {
        return engine.quote(request);
    }
}
