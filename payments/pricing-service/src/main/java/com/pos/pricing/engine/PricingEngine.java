package com.pos.pricing.engine;

import com.pos.pricing.domain.PricingProperties;
import com.pos.pricing.domain.Product;
import com.pos.pricing.domain.Promotion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic basket pricing. All money is integer cents; percentages and tax round half-up once per
 * line (discounts) or once per basket (tax) so the quote always reconciles to the cent.
 *
 * <p>Order of application: item-level promotions (category %, buy-X-get-Y) first, then order-level
 * threshold discounts (pro-rated across lines so refunds and tax stay correct), then tax on taxable lines.
 */
@Service
public class PricingEngine {

    private final Catalog catalog;
    private final PricingProperties properties;

    public PricingEngine(Catalog catalog, PricingProperties properties) {
        this.catalog = catalog;
        this.properties = properties;
    }

    public PriceQuote quote(QuoteRequest request) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (QuoteRequest.Item item : request.items()) {
            quantities.merge(item.sku(), item.quantity(), Integer::sum);
        }

        List<WorkingLine> lines = new ArrayList<>();
        quantities.forEach((sku, qty) -> {
            Product product = catalog.find(sku).orElseThrow(() -> new UnknownSkuException(sku));
            lines.add(new WorkingLine(product, qty));
        });

        Set<String> applied = new LinkedHashSet<>();
        for (Promotion promotion : properties.promotions()) {
            switch (promotion.type()) {
                case PERCENT_OFF_CATEGORY -> applyCategoryPercent(promotion, lines, applied);
                case BUY_X_GET_Y -> applyBuyXGetY(promotion, lines, applied);
                case ORDER_THRESHOLD -> { /* applied after item-level promotions */ }
            }
        }
        for (Promotion promotion : properties.promotions()) {
            if (promotion.type() == Promotion.Type.ORDER_THRESHOLD) {
                applyThreshold(promotion, lines, applied);
            }
        }

        long subtotal = lines.stream().mapToLong(WorkingLine::gross).sum();
        long discount = lines.stream().mapToLong(l -> l.discount).sum();
        long taxableNet = lines.stream().filter(l -> l.product.taxable()).mapToLong(WorkingLine::net).sum();
        BigDecimal taxRate = properties.taxRateFor(request.storeId());
        long tax = BigDecimal.valueOf(taxableNet).multiply(taxRate).setScale(0, RoundingMode.HALF_UP).longValueExact();

        List<PriceQuote.Line> quoteLines = lines.stream()
                .map(l -> new PriceQuote.Line(l.product.sku(), l.product.name(), l.product.category(), l.quantity,
                        l.product.priceCents(), l.discount, l.net(), l.product.taxable()))
                .toList();
        return new PriceQuote(request.storeId(), quoteLines, subtotal, discount, tax, subtotal - discount + tax,
                taxRate, List.copyOf(applied));
    }

    private static void applyCategoryPercent(Promotion promo, List<WorkingLine> lines, Set<String> applied) {
        for (WorkingLine line : lines) {
            if (line.product.category().equalsIgnoreCase(promo.category())) {
                long off = percentOf(line.net(), promo.percent());
                if (off > 0) {
                    line.discount += off;
                    applied.add(promo.id());
                }
            }
        }
    }

    private static void applyBuyXGetY(Promotion promo, List<WorkingLine> lines, Set<String> applied) {
        int groupSize = promo.buyQuantity() + promo.getQuantity();
        for (WorkingLine line : lines) {
            if (line.product.sku().equals(promo.sku()) && groupSize > 0) {
                long freeUnits = (long) (line.quantity / groupSize) * promo.getQuantity();
                long off = Math.min(freeUnits * line.product.priceCents(), line.net());
                if (off > 0) {
                    line.discount += off;
                    applied.add(promo.id());
                }
            }
        }
    }

    private static void applyThreshold(Promotion promo, List<WorkingLine> lines, Set<String> applied) {
        long net = lines.stream().mapToLong(WorkingLine::net).sum();
        if (net < promo.thresholdCents() || net == 0) {
            return;
        }
        long amount = Math.min(promo.amountOffCents(), net);
        long allocated = 0;
        for (int i = 0; i < lines.size(); i++) {
            WorkingLine line = lines.get(i);
            long share = i == lines.size() - 1
                    ? amount - allocated
                    : BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(line.net()))
                        .divide(BigDecimal.valueOf(net), 0, RoundingMode.HALF_UP).longValueExact();
            share = Math.min(share, line.net());
            line.discount += share;
            allocated += share;
        }
        applied.add(promo.id());
    }

    private static long percentOf(long cents, int percent) {
        return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact();
    }

    private static final class WorkingLine {
        final Product product;
        final int quantity;
        long discount;

        WorkingLine(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        long gross() {
            return product.priceCents() * quantity;
        }

        long net() {
            return gross() - discount;
        }
    }
}
