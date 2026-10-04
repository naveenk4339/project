package com.pos.payment.api;

import com.pos.common.web.ApiError;
import com.pos.payment.api.PaymentDtos.AuthorizeRequest;
import com.pos.payment.api.PaymentDtos.PaymentView;
import com.pos.payment.api.PaymentDtos.RefundRequest;
import com.pos.payment.api.PaymentDtos.RefundView;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /** Returns 200 for both approvals and declines; the {@code status} field carries the outcome. */
    @PostMapping("/authorize")
    public PaymentView authorize(@Valid @RequestBody AuthorizeRequest request) {
        return PaymentView.of(service.authorize(request));
    }

    @GetMapping("/{id}")
    public PaymentView get(@PathVariable UUID id) {
        return PaymentView.of(service.get(id));
    }

    @GetMapping
    public PaymentView byTransaction(@RequestParam String transactionId) {
        return PaymentView.of(service.byTransaction(transactionId));
    }

    @PostMapping("/{id}/refunds")
    public RefundView refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return service.refund(id, request);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiError> handle(PaymentException e) {
        return ResponseEntity.status(e.status()).body(ApiError.of(e.code(), e.getMessage()));
    }
}
