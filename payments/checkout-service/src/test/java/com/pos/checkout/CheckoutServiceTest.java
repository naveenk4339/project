package com.pos.checkout;

import com.pos.checkout.api.CheckoutDtos.CheckoutRequest;
import com.pos.checkout.api.CheckoutDtos.Tender;
import com.pos.checkout.api.CheckoutException;
import com.pos.checkout.api.CheckoutService;
import com.pos.checkout.client.CartClient;
import com.pos.checkout.client.ClientDtos.AuthorizeRequest;
import com.pos.checkout.client.ClientDtos.CartSnapshot;
import com.pos.checkout.client.ClientDtos.PaymentResult;
import com.pos.checkout.client.ClientDtos.Quote;
import com.pos.checkout.client.ClientDtos.RefundResult;
import com.pos.checkout.client.PaymentClient;
import com.pos.checkout.client.PricingClient;
import com.pos.checkout.domain.PosTransaction;
import com.pos.checkout.domain.TransactionStatus;
import com.pos.checkout.outbox.OutboxEvent;
import com.pos.checkout.outbox.OutboxRepository;
import com.pos.common.events.EventJson;
import com.pos.common.events.EventTypes;
import com.pos.common.events.TransactionCompleted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutServiceTest {

    @Autowired
    CheckoutService service;
    @Autowired
    OutboxRepository outbox;

    @MockitoBean
    CartClient carts;
    @MockitoBean
    PricingClient pricing;
    @MockitoBean
    PaymentClient payments;

    private String cartId;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID().toString();
        when(carts.lock(cartId)).thenReturn(new CartSnapshot(cartId, "store-001", "t1", "cust-42", "LOCKED",
                List.of(new CartSnapshot.Item("COF-001", 2))));
        when(pricing.quote(any())).thenReturn(new Quote(
                List.of(new Quote.Line("COF-001", "Drip Coffee", "beverage", 2, 299, 0, 598)),
                598, 0, 0, 598, List.of()));
    }

    private static PaymentResult captured(String txId) {
        return new PaymentResult("pay-1", txId, "CARD", "CAPTURED", 598, 598, 0, "VISA", "4242", "fp", "123456", null, 0.1);
    }

    @Test
    void completedCheckoutWritesExactlyOneOutboxEvent() throws Exception {
        when(payments.authorize(any())).thenAnswer(inv -> captured(inv.<AuthorizeRequest>getArgument(0).transactionId()));
        CheckoutRequest request = new CheckoutRequest(cartId, new Tender("CARD", "tok_visa", 0));

        PosTransaction tx = service.checkout("key-" + cartId, request);
        PosTransaction replay = service.checkout("key-" + cartId, request);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(replay.getId()).isEqualTo(tx.getId());
        verify(payments, times(1)).authorize(any());
        verify(carts).complete(cartId);

        List<OutboxEvent> events = outbox.findByAggregateIdOrderByCreatedAt(tx.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo(EventTypes.TRANSACTION_COMPLETED);
        TransactionCompleted payload = EventJson.mapper().readValue(events.getFirst().getPayload(), TransactionCompleted.class);
        assertThat(payload.totalCents()).isEqualTo(598);
        assertThat(payload.customerId()).isEqualTo("cust-42");
        assertThat(payload.payment().cardLast4()).isEqualTo("4242");
    }

    @Test
    void declinedCheckoutUnlocksCartAndEmitsNothing() {
        when(payments.authorize(any())).thenAnswer(inv -> new PaymentResult("pay-2",
                inv.<AuthorizeRequest>getArgument(0).transactionId(), "CARD",
                "DECLINED", 598, 0, 0, "VISA", "0002", "fp", null, "INSUFFICIENT_FUNDS", 0.1));

        PosTransaction tx = service.checkout("key-" + cartId, new CheckoutRequest(cartId, new Tender("CARD", "tok_decline", 0)));

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(tx.getDeclineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        verify(carts).unlock(cartId);
        assertThat(outbox.findByAggregateIdOrderByCreatedAt(tx.getId())).isEmpty();
    }

    @Test
    void paymentTimeoutLeavesTransactionPendingAndRetryResumes() {
        CheckoutRequest request = new CheckoutRequest(cartId, new Tender("CARD", "tok_visa", 0));
        when(payments.authorize(any())).thenThrow(new ResourceAccessException("read timed out"));

        assertThatThrownBy(() -> service.checkout("key-" + cartId, request))
                .isInstanceOf(CheckoutException.class).hasMessageContaining("retry");

        doAnswer(inv -> captured(inv.<AuthorizeRequest>getArgument(0).transactionId())).when(payments).authorize(any());
        PosTransaction tx = service.checkout("key-" + cartId, request);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(carts, times(1)).lock(cartId); // the retry resumed, it did not re-price or re-lock
    }

    @Test
    void refundEmitsRefundEventOnce() {
        when(payments.authorize(any())).thenAnswer(inv -> captured(inv.<AuthorizeRequest>getArgument(0).transactionId()));
        when(payments.refund(anyString(), any())).thenReturn(new RefundResult("ref-1", "pay-1", 598, "REFUNDED"));
        PosTransaction tx = service.checkout("key-" + cartId, new CheckoutRequest(cartId, new Tender("CARD", "tok_visa", 0)));

        service.refund(tx.getId(), "refund-" + tx.getId(), "customer changed mind");
        PosTransaction again = service.refund(tx.getId(), "refund-" + tx.getId(), "customer changed mind");

        assertThat(again.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(outbox.findByAggregateIdOrderByCreatedAt(tx.getId()))
                .extracting(OutboxEvent::getEventType)
                .containsExactly(EventTypes.TRANSACTION_COMPLETED, EventTypes.TRANSACTION_REFUNDED);
    }

    @Test
    void idempotencyKeyCannotBeReusedForAnotherCart() {
        when(payments.authorize(any())).thenAnswer(inv -> captured(inv.<AuthorizeRequest>getArgument(0).transactionId()));
        service.checkout("shared-key-" + cartId, new CheckoutRequest(cartId, new Tender("CARD", "tok_visa", 0)));

        assertThatThrownBy(() -> service.checkout("shared-key-" + cartId,
                new CheckoutRequest("other-cart", new Tender("CARD", "tok_visa", 0))))
                .isInstanceOf(CheckoutException.class).hasMessageContaining("already used");
    }
}
