package com.pos.inventory;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionEventHandler;
import com.pos.common.events.TransactionLine;
import com.pos.common.events.TransactionRefunded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class InventoryEventHandler implements TransactionEventHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventHandler.class);

    private final StockRepository stock;
    private final ProcessedEvents processed;

    public InventoryEventHandler(StockRepository stock, ProcessedEvents processed) {
        this.stock = stock;
        this.processed = processed;
    }

    @Override
    @Transactional
    public void onCompleted(EventEnvelope envelope, TransactionCompleted event) {
        if (processed.claim(envelope.eventId())) {
            apply(event.lines(), -1);
        }
    }

    @Override
    @Transactional
    public void onRefunded(EventEnvelope envelope, TransactionRefunded event) {
        if (processed.claim(envelope.eventId())) {
            apply(event.lines(), +1);
        }
    }

    private void apply(List<TransactionLine> lines, int sign) {
        for (TransactionLine line : lines) {
            StockItem item = stock.findById(line.sku()).orElseGet(() -> stock.save(new StockItem(line.sku(), 0, 0)));
            item.adjust(sign * line.quantity());
            if (item.belowReorderPoint()) {
                log.info("Low stock: {} on hand {} (reorder point {})", item.getSku(), item.getOnHand(), item.getReorderPoint());
            }
        }
    }
}
