package com.pos.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "stock")
public class StockItem {

    @Id
    private String sku;
    @Column(name = "on_hand", nullable = false)
    private int onHand;
    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected StockItem() {
    }

    public StockItem(String sku, int onHand, int reorderPoint) {
        this.sku = sku;
        this.onHand = onHand;
        this.reorderPoint = reorderPoint;
        this.updatedAt = Instant.now();
    }

    /** Stock may go negative: the sale already happened at the till, so it is recorded and flagged, not refused. */
    public void adjust(int delta) {
        onHand += delta;
        updatedAt = Instant.now();
    }

    public boolean belowReorderPoint() {
        return onHand <= reorderPoint;
    }

    public String getSku() { return sku; }
    public int getOnHand() { return onHand; }
    public int getReorderPoint() { return reorderPoint; }
    public Instant getUpdatedAt() { return updatedAt; }
}
