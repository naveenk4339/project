package com.pos.cart.api;

import com.pos.cart.domain.Cart;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CartDtos {

    private CartDtos() {
    }

    public record CreateCartRequest(@NotBlank String storeId, @NotBlank String terminalId, String customerId) {
    }

    public record AddItemRequest(@NotBlank String sku, @Min(1) int quantity) {
    }

    public record UpdateQuantityRequest(@Min(0) int quantity) {
    }

    public record AssignCustomerRequest(String customerId) {
    }

    public record CartView(UUID id, String storeId, String terminalId, String customerId, String status,
                           List<Item> items, Instant updatedAt) {

        public record Item(String sku, int quantity) {
        }

        public static CartView of(Cart cart) {
            return new CartView(cart.getId(), cart.getStoreId(), cart.getTerminalId(), cart.getCustomerId(),
                    cart.getStatus().name(),
                    cart.getItems().stream().map(i -> new Item(i.getSku(), i.getQuantity())).toList(),
                    cart.getUpdatedAt());
        }
    }
}
