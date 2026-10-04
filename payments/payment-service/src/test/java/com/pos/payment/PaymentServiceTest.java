package com.pos.payment;

import com.pos.payment.api.PaymentDtos.AuthorizeRequest;
import com.pos.payment.api.PaymentDtos.RefundRequest;
import com.pos.payment.api.PaymentException;
import com.pos.payment.api.PaymentService;
import com.pos.payment.domain.Payment;
import com.pos.payment.domain.PaymentMethod;
import com.pos.payment.domain.PaymentStatus;
import com.pos.payment.fraud.FraudClient;
import com.pos.payment.fraud.FraudClient.FraudScore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    PaymentService service;

    @MockitoBean
    FraudClient fraud;

    private static String txId() {
        return "tx-" + UUID.randomUUID();
    }

    private void fraudScore(double score) {
        when(fraud.score(any())).thenReturn(Optional.of(new FraudScore(score, "x", List.of())));
    }

    @Test
    void cardApprovalIsIdempotentPerTransaction() {
        fraudScore(0.05);
        String tx = txId();
        AuthorizeRequest request = new AuthorizeRequest(tx, "store-001", PaymentMethod.CARD, 1299, "tok_visa", 0);

        Payment first = service.authorize(request);
        Payment retry = service.authorize(request);

        assertThat(first.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(first.getAuthCode()).isNotBlank();
        assertThat(first.getCardLast4()).hasSize(4);
        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(retry.getAuthCode()).isEqualTo(first.getAuthCode());
    }

    @Test
    void processorDecline() {
        fraudScore(0.05);
        Payment p = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CARD, 500, "tok_decline", 0));
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(p.getDeclineReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void highFraudScoreDeclinesBeforeProcessor() {
        fraudScore(0.97);
        Payment p = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CARD, 500, "tok_visa", 0));
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(p.getDeclineReason()).isEqualTo("SUSPECTED_FRAUD");
        assertThat(p.getAuthCode()).isNull();
    }

    @Test
    void fraudOutageFailsOpenOnlyForSmallTickets() {
        when(fraud.score(any())).thenReturn(Optional.empty());
        Payment small = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CARD, 2000, "tok_visa", 0));
        Payment large = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CARD, 50000, "tok_visa", 0));
        assertThat(small.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(large.getDeclineReason()).isEqualTo("FRAUD_CHECK_UNAVAILABLE");
    }

    @Test
    void cashComputesChange() {
        Payment p = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CASH, 1234, null, 2000));
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(p.getChangeCents()).isEqualTo(766);

        Payment short_ = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CASH, 1234, null, 1000));
        assertThat(short_.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void refundsAreIdempotentAndBounded() {
        fraudScore(0.05);
        Payment p = service.authorize(new AuthorizeRequest(txId(), "store-001", PaymentMethod.CARD, 1000, "tok_visa", 0));

        var partial = service.refund(p.getId(), new RefundRequest("r-1", 400, "damaged"));
        var replay = service.refund(p.getId(), new RefundRequest("r-1", 400, "damaged"));
        assertThat(replay.refundId()).isEqualTo(partial.refundId());
        assertThat(partial.paymentStatus()).isEqualTo("PARTIALLY_REFUNDED");

        assertThatThrownBy(() -> service.refund(p.getId(), new RefundRequest("r-2", 700, null)))
                .isInstanceOf(PaymentException.class);
        assertThat(service.refund(p.getId(), new RefundRequest("r-3", 600, null)).paymentStatus()).isEqualTo("REFUNDED");
    }
}
