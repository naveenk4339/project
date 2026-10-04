package com.pos.ai.recommendation;

import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Item-to-item "frequently bought together" model. Similarity is the cosine of basket co-occurrence:
 * {@code co(a,b) / sqrt(n(a) * n(b))}. Falls back to overall popularity when an item has no co-purchases yet.
 */
@Component
public class CoPurchaseRecommender {

    private final Map<String, LongAdder> itemCounts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, LongAdder>> coCounts = new ConcurrentHashMap<>();
    private final Map<String, String> names = new ConcurrentHashMap<>();

    public void learn(TransactionCompleted sale) {
        Set<String> basket = sale.lines().stream().map(TransactionLine::sku).collect(Collectors.toSet());
        sale.lines().forEach(l -> names.put(l.sku(), l.name()));
        for (String a : basket) {
            itemCounts.computeIfAbsent(a, k -> new LongAdder()).increment();
            for (String b : basket) {
                if (!a.equals(b)) {
                    coCounts.computeIfAbsent(a, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(b, k -> new LongAdder()).increment();
                }
            }
        }
    }

    public List<Recommendation> forBasket(Collection<String> skus, int limit) {
        Map<String, Double> scores = new java.util.HashMap<>();
        for (String a : skus) {
            Map<String, LongAdder> co = coCounts.getOrDefault(a, Map.of());
            long na = count(a);
            co.forEach((b, c) -> {
                if (!skus.contains(b)) {
                    scores.merge(b, c.sum() / Math.sqrt((double) na * count(b)), Double::sum);
                }
            });
        }
        if (scores.isEmpty()) {
            itemCounts.forEach((sku, n) -> {
                if (!skus.contains(sku)) {
                    scores.put(sku, 0.01 * n.sum()); // popularity fallback, scaled below real similarities
                }
            });
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(e -> new Recommendation(e.getKey(), names.getOrDefault(e.getKey(), e.getKey()),
                        Math.round(e.getValue() * 1000) / 1000.0))
                .toList();
    }

    private long count(String sku) {
        LongAdder n = itemCounts.get(sku);
        return n == null ? 1 : Math.max(1, n.sum());
    }

    public record Recommendation(String sku, String name, double score) {
    }
}
