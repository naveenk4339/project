package com.pos.analytics;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionEventHandler;
import com.pos.common.events.TransactionLine;
import com.pos.common.events.TransactionRefunded;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streaming aggregates per store (gross/net sales, tickets, refunds, units by SKU, sales by hour and tender).
 * State is in memory and rebuilt by replaying the topic from the beginning on start-up
 * (consumer group with {@code auto-offset-reset: earliest} and a fresh group id per instance).
 */
@Component
public class SalesAggregator implements TransactionEventHandler {

    private final Map<String, StoreStats> stores = new ConcurrentHashMap<>();
    private final Set<UUID> seen = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
                    return size() > 100_000;
                }
            }));

    @Override
    public void onCompleted(EventEnvelope envelope, TransactionCompleted sale) {
        if (seen.add(envelope.eventId())) {
            stores.computeIfAbsent(sale.storeId(), StoreStats::new).add(sale);
        }
    }

    @Override
    public void onRefunded(EventEnvelope envelope, TransactionRefunded refund) {
        if (seen.add(envelope.eventId())) {
            stores.computeIfAbsent(refund.storeId(), StoreStats::new).refund(refund);
        }
    }

    public Summary summary(String storeId) {
        StoreStats stats = stores.get(storeId);
        return stats == null ? Summary.empty(storeId) : stats.snapshot();
    }

    public List<Summary> all() {
        return stores.values().stream().map(StoreStats::snapshot).toList();
    }

    public record Summary(String storeId, long grossSalesCents, long refundsCents, long netSalesCents,
                          long taxCents, long discountCents, int transactions, int refundCount,
                          long averageTicketCents, List<SkuUnits> topSkus, Map<Integer, Long> salesByHourUtc,
                          Map<String, Long> salesByTender) {

        static Summary empty(String storeId) {
            return new Summary(storeId, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), Map.of(), Map.of());
        }
    }

    public record SkuUnits(String sku, String name, long units, long revenueCents) {
    }

    private static final class StoreStats {
        private final String storeId;
        private long gross;
        private long refunds;
        private long tax;
        private long discount;
        private int transactions;
        private int refundCount;
        private final Map<String, SkuUnits> skus = new TreeMap<>();
        private final Map<Integer, Long> byHour = new TreeMap<>();
        private final Map<String, Long> byTender = new TreeMap<>();

        StoreStats(String storeId) {
            this.storeId = storeId;
        }

        synchronized void add(TransactionCompleted sale) {
            gross += sale.totalCents();
            tax += sale.taxCents();
            discount += sale.discountCents();
            transactions++;
            byHour.merge(sale.completedAt().atZone(ZoneOffset.UTC).getHour(), sale.totalCents(), Long::sum);
            String tender = sale.payment() == null ? "UNKNOWN" : sale.payment().method();
            byTender.merge(tender, sale.totalCents(), Long::sum);
            for (TransactionLine l : sale.lines()) {
                skus.merge(l.sku(), new SkuUnits(l.sku(), l.name(), l.quantity(), l.lineTotalCents()),
                        (a, b) -> new SkuUnits(a.sku(), a.name(), a.units() + b.units(), a.revenueCents() + b.revenueCents()));
            }
        }

        synchronized void refund(TransactionRefunded refund) {
            refunds += refund.refundedCents();
            refundCount++;
            for (TransactionLine l : refund.lines()) {
                skus.computeIfPresent(l.sku(), (k, a) -> new SkuUnits(a.sku(), a.name(), a.units() - l.quantity(),
                        a.revenueCents() - l.lineTotalCents()));
            }
        }

        synchronized Summary snapshot() {
            List<SkuUnits> top = skus.values().stream()
                    .sorted(Comparator.comparingLong(SkuUnits::units).reversed()).limit(5).toList();
            return new Summary(storeId, gross, refunds, gross - refunds, tax, discount, transactions, refundCount,
                    transactions == 0 ? 0 : gross / transactions, top, Map.copyOf(byHour), Map.copyOf(byTender));
        }
    }
}
