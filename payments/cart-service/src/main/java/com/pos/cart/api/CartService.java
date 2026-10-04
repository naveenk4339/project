package com.pos.cart.api;

import com.pos.cart.client.CatalogClient;
import com.pos.cart.domain.Cart;
import com.pos.cart.domain.CartNotFoundException;
import com.pos.cart.domain.CartRepository;
import com.pos.cart.domain.CartStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
public class CartService {

    private final CartRepository carts;
    private final CatalogClient catalog;

    public CartService(CartRepository carts, CatalogClient catalog) {
        this.carts = carts;
        this.catalog = catalog;
    }

    @Transactional
    public Cart create(String storeId, String terminalId, String customerId) {
        return carts.save(new Cart(storeId, terminalId, customerId));
    }

    @Transactional(readOnly = true)
    public Cart get(UUID id) {
        return carts.findById(id).orElseThrow(() -> new CartNotFoundException(id));
    }

    @Transactional
    public Cart addItem(UUID id, String sku, int quantity) {
        if (!catalog.exists(sku)) {
            throw new CartStateException("Unknown SKU: " + sku);
        }
        return mutate(id, cart -> cart.addItem(sku, quantity));
    }

    @Transactional
    public Cart setQuantity(UUID id, String sku, int quantity) {
        return mutate(id, cart -> cart.setItemQuantity(sku, quantity));
    }

    @Transactional
    public Cart assignCustomer(UUID id, String customerId) {
        return mutate(id, cart -> cart.assignCustomer(customerId));
    }

    @Transactional
    public Cart lock(UUID id) {
        return mutate(id, Cart::lock);
    }

    @Transactional
    public Cart unlock(UUID id) {
        return mutate(id, Cart::unlock);
    }

    @Transactional
    public Cart complete(UUID id) {
        return mutate(id, Cart::complete);
    }

    private Cart mutate(UUID id, Consumer<Cart> change) {
        Cart cart = get(id);
        change.accept(cart);
        return carts.save(cart);
    }
}
