package com.pos.checkout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TransactionLineItem {

    @Column(nullable = false)
    private String sku;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;
    @Column(name = "discount_cents", nullable = false)
    private long discountCents;
    @Column(name = "line_total_cents", nullable = false)
    private long lineTotalCents;

    protected TransactionLineItem() {
    }

    public TransactionLineItem(String sku, String name, String category, int quantity, long unitPriceCents,
                               long discountCents, long lineTotalCents) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.discountCents = discountCents;
        this.lineTotalCents = lineTotalCents;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public long getDiscountCents() { return discountCents; }
    public long getLineTotalCents() { return lineTotalCents; }
}
