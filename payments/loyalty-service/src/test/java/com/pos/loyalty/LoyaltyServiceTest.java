package com.pos.loyalty;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionRefunded;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoyaltyServiceTest {

    @Autowired
    LoyaltyService loyalty;

    private static EventEnvelope envelope() {
        return new EventEnvelope(UUID.randomUUID(), "x", "PosTransaction", "TX", Instant.now(), null);
    }

    private static TransactionCompleted sale(String customer, String tx, long totalCents, long taxCents) {
        return new TransactionCompleted(tx, "store-001", "t1", customer, List.of(), totalCents, 0, taxCents,
                totalCents, List.of(), null, Instant.now());
    }

    @Test
    void earnsPointsIdempotentlyAndReversesOnRefund() {
        String customer = "cust-" + UUID.randomUUID();
        EventEnvelope sale = envelope();
        loyalty.onCompleted(sale, sale(customer, "TX-A", 10_650, 600)); // $100.50 pre-tax -> 100 pts
        loyalty.onCompleted(sale, sale(customer, "TX-A", 10_650, 600)); // redelivery

        assertThat(loyalty.account(customer).orElseThrow().points()).isEqualTo(100);

        loyalty.onRefunded(envelope(), new TransactionRefunded("TX-A", "store-001", customer, "p", "r",
                List.of(), 10_650, null, Instant.now()));
        var account = loyalty.account(customer).orElseThrow();
        assertThat(account.points()).isZero();
        assertThat(account.lifetimePoints()).isEqualTo(100);
        assertThat(account.recent()).hasSize(2);
    }

    @Test
    void goldMembersEarnDouble() {
        String customer = "cust-" + UUID.randomUUID();
        loyalty.onCompleted(envelope(), sale(customer, "TX-1", 2_000_00, 0)); // 2000 pts -> GOLD
        assertThat(loyalty.account(customer).orElseThrow().tier()).isEqualTo("GOLD");

        loyalty.onCompleted(envelope(), sale(customer, "TX-2", 10_00, 0));
        assertThat(loyalty.account(customer).orElseThrow().points()).isEqualTo(2020);
    }

    @Test
    void anonymousSalesAreIgnored() {
        loyalty.onCompleted(envelope(), sale(null, "TX-anon", 5_00, 0));
        assertThat(loyalty.account("null")).isEmpty();
    }
}
