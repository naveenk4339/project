package com.pos.cart.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cart")
public class Cart {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "terminal_id", nullable = false)
    private String terminalId;

    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_item", joinColumns = @JoinColumn(name = "cart_id"))
    @OrderColumn(name = "position")
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Cart() {
    }

    public Cart(String storeId, String terminalId, String customerId) {
        this.id = UUID.randomUUID();
        this.storeId = storeId;
        this.terminalId = terminalId;
        this.customerId = customerId;
        this.status = CartStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void addItem(String sku, int quantity) {
        requireStatus(CartStatus.OPEN);
        items.stream().filter(i -> i.getSku().equals(sku)).findFirst()
                .ifPresentOrElse(i -> i.setQuantity(i.getQuantity() + quantity),
                        () -> items.add(new CartItem(sku, quantity)));
        touch();
    }

    public void setItemQuantity(String sku, int quantity) {
        requireStatus(CartStatus.OPEN);
        if (quantity <= 0) {
            items.removeIf(i -> i.getSku().equals(sku));
        } else {
            CartItem item = items.stream().filter(i -> i.getSku().equals(sku)).findFirst()
                    .orElseThrow(() -> new CartStateException("SKU " + sku + " is not in the cart"));
            item.setQuantity(quantity);
        }
        touch();
    }

    public void assignCustomer(String customerId) {
        requireStatus(CartStatus.OPEN);
        this.customerId = customerId;
        touch();
    }

    public void lock() {
        requireStatus(CartStatus.OPEN);
        if (items.isEmpty()) {
            throw new CartStateException("Cannot check out an empty cart");
        }
        status = CartStatus.LOCKED;
        touch();
    }

    public void unlock() {
        if (status == CartStatus.LOCKED) {
            status = CartStatus.OPEN;
            touch();
        }
    }

    public void complete() {
        if (status == CartStatus.CHECKED_OUT) {
            return;
        }
        requireStatus(CartStatus.LOCKED);
        status = CartStatus.CHECKED_OUT;
        touch();
    }

    private void requireStatus(CartStatus expected) {
        if (status != expected) {
            throw new CartStateException("Cart " + id + " is " + status + ", expected " + expected);
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
