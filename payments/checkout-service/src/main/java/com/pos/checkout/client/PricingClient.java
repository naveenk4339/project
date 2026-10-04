package com.pos.checkout.client;

import com.pos.checkout.client.ClientDtos.Quote;
import com.pos.checkout.client.ClientDtos.QuoteRequest;
import org.springframework.web.client.RestClient;

public class PricingClient {

    private final RestClient rest;

    public PricingClient(RestClient rest) {
        this.rest = rest;
    }

    public Quote quote(QuoteRequest request) {
        return rest.post().uri("/api/pricing/quote").body(request).retrieve().body(Quote.class);
    }
}
