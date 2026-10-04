package com.pos.ai.forecast;

import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import com.pos.common.events.TransactionRefunded;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Per-SKU daily demand forecast using Holt's linear exponential smoothing (level + trend).
 * Days with no sales inside the observed range count as zero demand.
 */
@Component
public class DemandForecaster {

    static final double ALPHA = 0.5;
    static final double BETA = 0.3;

    private final Map<String, NavigableMap<LocalDate, Long>> daily = new ConcurrentHashMap<>();
    private final Clock clock;

    public DemandForecaster() {
        this(Clock.systemUTC());
    }

    DemandForecaster(Clock clock) {
        this.clock = clock;
    }

    public void recordSale(TransactionCompleted sale) {
        LocalDate day = sale.completedAt().atZone(ZoneOffset.UTC).toLocalDate();
        for (TransactionLine line : sale.lines()) {
            series(line.sku()).merge(day, (long) line.quantity(), Long::sum);
        }
    }

    public void recordRefund(TransactionRefunded refund) {
        LocalDate day = refund.refundedAt().atZone(ZoneOffset.UTC).toLocalDate();
        for (TransactionLine line : refund.lines()) {
            series(line.sku()).merge(day, (long) -line.quantity(), Long::sum);
        }
    }

    /** Test/demo hook for loading historical demand. */
    public void recordUnits(String sku, LocalDate day, long units) {
        series(sku).merge(day, units, Long::sum);
    }

    public Forecast forecast(String sku, int days) {
        NavigableMap<LocalDate, Long> series = daily.getOrDefault(sku, new ConcurrentSkipListMap<>());
        List<DayUnits> history = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        if (!series.isEmpty()) {
            for (LocalDate d = series.firstKey(); !d.isAfter(today); d = d.plusDays(1)) {
                history.add(new DayUnits(d, Math.max(0, series.getOrDefault(d, 0L))));
            }
        }

        double level;
        double trend = 0;
        if (history.isEmpty()) {
            level = 0;
        } else if (history.size() == 1) {
            level = history.getFirst().units();
        } else {
            level = history.get(0).units();
            trend = history.get(1).units() - history.get(0).units();
            for (int i = 1; i < history.size(); i++) {
                double y = history.get(i).units();
                double previousLevel = level;
                level = ALPHA * y + (1 - ALPHA) * (level + trend);
                trend = BETA * (level - previousLevel) + (1 - BETA) * trend;
            }
        }

        List<DayUnits> forecast = new ArrayList<>();
        long total = 0;
        for (int h = 1; h <= days; h++) {
            long units = Math.max(0, Math.round(level + h * trend));
            forecast.add(new DayUnits(today.plusDays(h), units));
            total += units;
        }
        int keep = Math.min(history.size(), 30);
        return new Forecast(sku, history.subList(history.size() - keep, history.size()), forecast, total,
                "holt-linear(alpha=" + ALPHA + ",beta=" + BETA + ")");
    }

    private NavigableMap<LocalDate, Long> series(String sku) {
        return daily.computeIfAbsent(sku, k -> new ConcurrentSkipListMap<>());
    }

    public record DayUnits(LocalDate date, long units) {
    }

    public record Forecast(String sku, List<DayUnits> history, List<DayUnits> forecast, long totalForecastUnits,
                           String model) {
    }
}
