package com.pos.receipt;

import com.pos.common.events.PaymentSummary;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptRendererTest {

    @Test
    void rendersFixedWidthReceipt() {
        TransactionCompleted sale = new TransactionCompleted("TX-20261004-ABCD1234", "store-001", "t1", null,
                List.of(new TransactionLine("BAK-001", "Blueberry Muffin", "bakery", 2, 325, 130, 520)),
                650, 130, 0, 520, List.of("BAKERY-20"),
                new PaymentSummary("p", "CASH", null, null, null, null, 520, 1000, 480, 0), Instant.parse("2026-10-04T12:00:00Z"));

        String text = new ReceiptRenderer().render(sale, true);

        assertThat(text.lines()).allSatisfy(l -> assertThat(l.length()).isLessThanOrEqualTo(ReceiptRenderer.WIDTH));
        assertThat(text).contains("2 x Blueberry Muffin", "$6.50", "Savings", "-$1.30", "TOTAL", "$5.20",
                "Change", "$4.80", "BAKERY-20", "REFUNDED");
    }
}
