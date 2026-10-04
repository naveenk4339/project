package com.pos.pricing;

import com.pos.pricing.domain.PricingProperties;
import com.pos.pricing.domain.Product;
import com.pos.pricing.domain.Promotion;
import com.pos.pricing.engine.Catalog;
import com.pos.pricing.engine.PriceQuote;
import com.pos.pricing.engine.PricingEngine;
import com.pos.pricing.engine.QuoteRequest;
import com.pos.pricing.engine.UnknownSkuException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingEngineTest {

    private final PricingProperties properties = new PricingProperties(
            List.of(
                    new Product("BAK-001", "Muffin", "bakery", 325, false),
                    new Product("SNK-001", "Chips", "snacks", 199, true),
                    new Product("ELE-002", "Earbuds", "electronics", 4999, true)),
            List.of(
                    new Promotion("BAKERY-20", "", Promotion.Type.PERCENT_OFF_CATEGORY, "bakery", null, 20, 0, 0, 0, 0),
                    new Promotion("CHIPS-B2G1", "", Promotion.Type.BUY_X_GET_Y, null, "SNK-001", 0, 2, 1, 0, 0),
                    new Promotion("SPEND50-5", "", Promotion.Type.ORDER_THRESHOLD, null, null, 0, 0, 0, 5000, 500)),
            Map.of("store-001", new BigDecimal("0.06")),
            BigDecimal.ZERO);

    private final PricingEngine engine = new PricingEngine(new Catalog(properties), properties);

    @Test
    void appliesCategoryPercentAndTaxesOnlyTaxableLines() {
        PriceQuote quote = engine.quote(new QuoteRequest("store-001", List.of(
                new QuoteRequest.Item("BAK-001", 2),
                new QuoteRequest.Item("SNK-001", 1))));

        // muffins 650 - 20% (130) = 520, chips 199 taxable -> tax 11.94 -> 12
        assertThat(quote.subtotalCents()).isEqualTo(849);
        assertThat(quote.discountCents()).isEqualTo(130);
        assertThat(quote.taxCents()).isEqualTo(12);
        assertThat(quote.totalCents()).isEqualTo(849 - 130 + 12);
        assertThat(quote.appliedPromotions()).containsExactly("BAKERY-20");
    }

    @Test
    void buyTwoGetOneFreeAndMergesDuplicateSkus() {
        PriceQuote quote = engine.quote(new QuoteRequest("store-001", List.of(
                new QuoteRequest.Item("SNK-001", 2),
                new QuoteRequest.Item("SNK-001", 1))));

        assertThat(quote.lines()).hasSize(1);
        assertThat(quote.lines().getFirst().quantity()).isEqualTo(3);
        assertThat(quote.discountCents()).isEqualTo(199);
        assertThat(quote.appliedPromotions()).containsExactly("CHIPS-B2G1");
    }

    @Test
    void thresholdDiscountIsProRatedAndReconciles() {
        PriceQuote quote = engine.quote(new QuoteRequest("store-001", List.of(
                new QuoteRequest.Item("ELE-002", 1),
                new QuoteRequest.Item("BAK-001", 1))));

        long lineDiscounts = quote.lines().stream().mapToLong(PriceQuote.Line::discountCents).sum();
        assertThat(lineDiscounts).isEqualTo(quote.discountCents());
        assertThat(quote.appliedPromotions()).contains("BAKERY-20", "SPEND50-5");
        long lineTotals = quote.lines().stream().mapToLong(PriceQuote.Line::lineTotalCents).sum();
        assertThat(lineTotals + quote.taxCents()).isEqualTo(quote.totalCents());
    }

    @Test
    void rejectsUnknownSku() {
        assertThatThrownBy(() -> engine.quote(new QuoteRequest("store-001", List.of(new QuoteRequest.Item("NOPE", 1)))))
                .isInstanceOf(UnknownSkuException.class);
    }
}
