package com.pos.cart.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Validates SKUs against the pricing service's catalog before they go into a basket. */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder, @Value("${services.pricing-url}") String pricingUrl) {
        this.restClient = builder.baseUrl(pricingUrl).build();
    }

    public boolean exists(String sku) {
        try {
            restClient.get().uri("/api/products/{sku}", sku).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }
}
