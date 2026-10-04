package com.pos.cart.api;

import com.pos.cart.api.CartDtos.AddItemRequest;
import com.pos.cart.api.CartDtos.AssignCustomerRequest;
import com.pos.cart.api.CartDtos.CartView;
import com.pos.cart.api.CartDtos.CreateCartRequest;
import com.pos.cart.api.CartDtos.UpdateQuantityRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartView create(@Valid @RequestBody CreateCartRequest request) {
        return CartView.of(service.create(request.storeId(), request.terminalId(), request.customerId()));
    }

    @GetMapping("/{id}")
    public CartView get(@PathVariable UUID id) {
        return CartView.of(service.get(id));
    }

    @PostMapping("/{id}/items")
    public CartView addItem(@PathVariable UUID id, @Valid @RequestBody AddItemRequest request) {
        return CartView.of(service.addItem(id, request.sku(), request.quantity()));
    }

    @PutMapping("/{id}/items/{sku}")
    public CartView setQuantity(@PathVariable UUID id, @PathVariable String sku,
                                @Valid @RequestBody UpdateQuantityRequest request) {
        return CartView.of(service.setQuantity(id, sku, request.quantity()));
    }

    @DeleteMapping("/{id}/items/{sku}")
    public CartView removeItem(@PathVariable UUID id, @PathVariable String sku) {
        return CartView.of(service.setQuantity(id, sku, 0));
    }

    @PutMapping("/{id}/customer")
    public CartView assignCustomer(@PathVariable UUID id, @RequestBody AssignCustomerRequest request) {
        return CartView.of(service.assignCustomer(id, request.customerId()));
    }

    // --- lifecycle endpoints used by the checkout service ---

    @PostMapping("/{id}/lock")
    public CartView lock(@PathVariable UUID id) {
        return CartView.of(service.lock(id));
    }

    @PostMapping("/{id}/unlock")
    public CartView unlock(@PathVariable UUID id) {
        return CartView.of(service.unlock(id));
    }

    @PostMapping("/{id}/complete")
    public CartView complete(@PathVariable UUID id) {
        return CartView.of(service.complete(id));
    }
}
