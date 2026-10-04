package com.pos.ai.fraud;

import com.pos.common.events.TransactionCompleted;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Online feature store keyed by card fingerprint (a hash of the card token, never the PAN). Holds a sliding
 * window of authorization attempts plus each card's completed-sale history, both bounded in size.
 */
@Component
public class FraudFeatureStore {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_EVENTS = 200;

    private final Map<String, CardHistory> cards = new ConcurrentHashMap<>();
    private final Clock clock;

    public FraudFeatureStore() {
        this(Clock.systemUTC());
    }

    FraudFeatureStore(Clock clock) {
        this.clock = clock;
    }

    public void recordAttempt(String fingerprint, String storeId) {
        if (fingerprint != null) {
            cards.computeIfAbsent(fingerprint, k -> new CardHistory()).attempt(clock.instant(), storeId);
        }
    }

    public void recordSale(TransactionCompleted sale) {
        if (sale.payment() != null && sale.payment().cardFingerprint() != null) {
            cards.computeIfAbsent(sale.payment().cardFingerprint(), k -> new CardHistory()).sale(sale.totalCents());
        }
    }

    public FraudFeatures features(String fingerprint, String storeId, long amountCents) {
        CardHistory history = fingerprint == null ? null : cards.get(fingerprint);
        Instant now = clock.instant();
        int hour = now.atZone(java.time.ZoneOffset.UTC).getHour();
        if (history == null) {
            return new FraudFeatures(amountCents, 0, 1, true, 0, hour);
        }
        return history.features(now, storeId, amountCents, hour);
    }

    private static final class CardHistory {
        private final Deque<Attempt> attempts = new ArrayDeque<>();
        private long sales;
        private long totalSpendCents;

        synchronized void attempt(Instant at, String storeId) {
            attempts.addLast(new Attempt(at, storeId));
            while (attempts.size() > MAX_EVENTS) {
                attempts.removeFirst();
            }
        }

        synchronized void sale(long cents) {
            sales++;
            totalSpendCents += cents;
        }

        synchronized FraudFeatures features(Instant now, String storeId, long amountCents, int hour) {
            attempts.removeIf(a -> a.at().isBefore(now.minus(WINDOW)));
            int last10Min = (int) attempts.stream().filter(a -> a.at().isAfter(now.minus(Duration.ofMinutes(10)))).count();
            Set<String> stores = new HashSet<>();
            attempts.forEach(a -> stores.add(a.storeId()));
            stores.add(storeId);
            double avg = sales == 0 ? 0 : (double) totalSpendCents / sales;
            return new FraudFeatures(amountCents, last10Min, stores.size(), sales == 0,
                    avg == 0 ? 0 : amountCents / avg, hour);
        }
    }

    private record Attempt(Instant at, String storeId) {
    }
}
