package com.pos.analytics;

import com.pos.common.events.Topics;
import com.pos.common.events.TransactionEventDispatcher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final SalesAggregator aggregator;

    public AnalyticsController(SalesAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @GetMapping("/stores")
    public List<SalesAggregator.Summary> stores() {
        return aggregator.all();
    }

    @GetMapping("/stores/{storeId}")
    public SalesAggregator.Summary store(@PathVariable String storeId) {
        return aggregator.summary(storeId);
    }

    // random group id per instance => every replica replays the full topic to rebuild its in-memory view
    @KafkaListener(topics = Topics.TRANSACTION_EVENTS, groupId = "analytics-${random.uuid}")
    public void on(String message) {
        TransactionEventDispatcher.dispatch(message, aggregator);
    }
}
