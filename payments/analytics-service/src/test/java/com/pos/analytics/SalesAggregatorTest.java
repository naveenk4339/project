package com.pos.analytics;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.PaymentSummary;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import com.pos.common.events.TransactionRefunded;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SalesAggregatorTest {

    private static EventEnvelope env() {
        return new EventEnvelope(UUID.randomUUID(), "x", "PosTransaction", "TX", Instant.now(), null);
    }

    private static TransactionCompleted sale(String tx, String method, long total, String sku, int qty) {
        return new TransactionCompleted(tx, "store-001", "t1", null,
                List.of(new TransactionLine(sku, sku, "c", qty, total / qty, 0, total)),
                total, 0, 0, total, List.of(),
                new PaymentSummary("p", method, null, null, null, null, total, total, 0, 0),
                Instant.parse("2026-10-04T14:05:00Z"));
    }

    @Test
    void aggregatesSalesAndRefundsIgnoringDuplicates() {
        SalesAggregator agg = new SalesAggregator();
        EventEnvelope first = env();
        agg.onCompleted(first, sale("TX-1", "CARD", 1000, "COF-001", 2));
        agg.onCompleted(first, sale("TX-1", "CARD", 1000, "COF-001", 2)); // duplicate
        agg.onCompleted(env(), sale("TX-2", "CASH", 3000, "MER-001", 1));
        agg.onRefunded(env(), new TransactionRefunded("TX-2", "store-001", null, "p", "r",
                List.of(new TransactionLine("MER-001", "MER-001", "c", 1, 3000, 0, 3000)), 3000, null, Instant.now()));

        SalesAggregator.Summary s = agg.summary("store-001");
        assertThat(s.transactions()).isEqualTo(2);
        assertThat(s.grossSalesCents()).isEqualTo(4000);
        assertThat(s.netSalesCents()).isEqualTo(1000);
        assertThat(s.averageTicketCents()).isEqualTo(2000);
        assertThat(s.salesByTender()).containsEntry("CARD", 1000L).containsEntry("CASH", 3000L);
        assertThat(s.salesByHourUtc()).containsEntry(14, 4000L);
        assertThat(s.topSkus().getFirst().sku()).isEqualTo("COF-001");
    }
}
