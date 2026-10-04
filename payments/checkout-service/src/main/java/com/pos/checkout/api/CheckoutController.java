package com.pos.checkout.api;

import com.pos.checkout.api.CheckoutDtos.CheckoutRequest;
import com.pos.checkout.api.CheckoutDtos.RefundRequest;
import com.pos.checkout.api.CheckoutDtos.TransactionView;
import com.pos.checkout.client.UpstreamException;
import com.pos.common.web.ApiError;
import com.pos.checkout.domain.PosTransaction;
import com.pos.checkout.domain.TransactionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    /** 201 when paid, 402 when the payment was declined; the body is the transaction either way. */
    @PostMapping("/checkout")
    public ResponseEntity<TransactionView> checkout(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                    @Valid @RequestBody CheckoutRequest request) {
        PosTransaction transaction = service.checkout(idempotencyKey, request);
        HttpStatus status = transaction.getStatus() == TransactionStatus.DECLINED
                ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(TransactionView.of(transaction));
    }

    @GetMapping("/transactions/{id}")
    public TransactionView get(@PathVariable String id) {
        return TransactionView.of(service.get(id));
    }

    @GetMapping("/transactions")
    public List<TransactionView> recent(@RequestParam String storeId, @RequestParam(defaultValue = "20") int limit) {
        return service.recent(storeId, limit).stream().map(TransactionView::of).toList();
    }

    @PostMapping("/transactions/{id}/refund")
    public TransactionView refund(@PathVariable String id, @RequestHeader("Idempotency-Key") String idempotencyKey,
                                  @RequestBody(required = false) RefundRequest request) {
        String reason = request == null ? null : request.reason();
        return TransactionView.of(service.refund(id, idempotencyKey, reason));
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ApiError> checkoutError(CheckoutException e) {
        return ResponseEntity.status(e.status()).body(ApiError.of(e.code(), e.getMessage()));
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ApiError> upstreamError(UpstreamException e) {
        return ResponseEntity.status(e.status()).body(ApiError.of("UPSTREAM_REJECTED", e.getMessage()));
    }
}
