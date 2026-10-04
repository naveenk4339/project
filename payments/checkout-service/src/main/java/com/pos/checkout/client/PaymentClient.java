package com.pos.checkout.client;

import com.pos.checkout.client.ClientDtos.AuthorizeRequest;
import com.pos.checkout.client.ClientDtos.PaymentResult;
import com.pos.checkout.client.ClientDtos.RefundRequest;
import com.pos.checkout.client.ClientDtos.RefundResult;
import org.springframework.web.client.RestClient;

public class PaymentClient {

    private final RestClient rest;

    public PaymentClient(RestClient rest) {
        this.rest = rest;
    }

    public PaymentResult authorize(AuthorizeRequest request) {
        return rest.post().uri("/api/payments/authorize").body(request).retrieve().body(PaymentResult.class);
    }

    public RefundResult refund(String paymentId, RefundRequest request) {
        return rest.post().uri("/api/payments/{id}/refunds", paymentId).body(request).retrieve().body(RefundResult.class);
    }
}
