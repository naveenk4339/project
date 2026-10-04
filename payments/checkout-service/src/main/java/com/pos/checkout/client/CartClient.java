package com.pos.checkout.client;

import com.pos.checkout.client.ClientDtos.CartSnapshot;
import org.springframework.web.client.RestClient;

public class CartClient {

    private final RestClient rest;

    public CartClient(RestClient rest) {
        this.rest = rest;
    }

    public CartSnapshot get(String cartId) {
        return rest.get().uri("/api/carts/{id}", cartId).retrieve().body(CartSnapshot.class);
    }

    public CartSnapshot lock(String cartId) {
        return rest.post().uri("/api/carts/{id}/lock", cartId).retrieve().body(CartSnapshot.class);
    }

    public void unlock(String cartId) {
        rest.post().uri("/api/carts/{id}/unlock", cartId).retrieve().toBodilessEntity();
    }

    public void complete(String cartId) {
        rest.post().uri("/api/carts/{id}/complete", cartId).retrieve().toBodilessEntity();
    }
}
